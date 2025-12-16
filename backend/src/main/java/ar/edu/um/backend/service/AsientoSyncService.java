package ar.edu.um.backend.service;

import ar.edu.um.backend.domain.Asiento;
import ar.edu.um.backend.domain.Evento;
import ar.edu.um.backend.domain.enumeration.AsientoEstado;
import ar.edu.um.backend.repository.AsientoRepository;
import ar.edu.um.backend.service.dto.ProxyAsientoDTO;
import ar.edu.um.backend.service.dto.ProxyEstadoAsientosResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servicio encargado de sincronizar los asientos locales de un evento
 * con la información real proveniente de la cátedra (vía proxy).
 *
 * Estrategia (SAFE):
 *   1) Se obtienen los asientos reales desde el proxy (Redis remoto).
 *   2) Si el proxy devuelve vacío (o no hay key en Redis), NO se borra nada local.
 *   3) Se hace UPSERT por (fila, columna):
 *        - si existe asiento local → se actualiza estado/persona
 *        - si no existe → se crea
 *
 * Nota: Esta estrategia evita romper ventas (FK rel_venta__asientos) porque NO borra asientos ya referenciados.
 */
@Service
@Transactional
public class AsientoSyncService {

    private static final Logger log = LoggerFactory.getLogger(AsientoSyncService.class);

    private final ProxyService proxyService;
    private final AsientoRepository asientoRepository;
    private final ObjectMapper objectMapper;

    public AsientoSyncService(ProxyService proxyService, AsientoRepository asientoRepository, ObjectMapper objectMapper) {
        this.proxyService = proxyService;
        this.asientoRepository = asientoRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Sincroniza asientos de un evento concreto desde el proxy.
     *
     * Flujo:
     *  - Llama al proxy para obtener el JSON de estado de asientos del evento.
     *  - Parsea el JSON a una lista de {@link ProxyAsientoDTO}.
     *  - Si la lista viene vacía → se asume "no hay estado remoto" y se mantiene estado local.
     *  - Si hay datos → se actualiza/crea por (fila,columna) sin borrar.
     *
     * @param eventoLocal Evento local (ya persistido) sobre el cual se sincronizan los asientos.
     * @param externalId  ID del evento en la cátedra (usado para consultar al proxy).
     */
    public void sincronizarAsientosDeEvento(Evento eventoLocal, Long externalId) {
        log.info(
            "🔄 [Sync-Asientos] Iniciando sincronización de asientos para evento local id={} (externalId={})",
            eventoLocal.getId(),
            externalId
        );

        // 1) Obtener JSON crudo desde el proxy
        String json = proxyService.listarAsientosDeEvento(externalId);
        if (json == null) {
            log.warn(
                "⚠️  [Sync-Asientos] No se pudo obtener la lista de asientos desde el proxy para externalId={}. Se mantiene estado local.",
                externalId
            );
            return;
        }

        // 2) Convertir JSON → lista de DTOs de asientos remotos
        List<ProxyAsientoDTO> remotos = parsearLista(json);

        // IMPORTANTE: lista vacía NO significa “evento sin asientos”, sino “no hay estado remoto disponible”
        if (remotos.isEmpty()) {
            log.warn(
                "⚠️  [Sync-Asientos] Proxy devolvió lista vacía para externalId={}. Se mantiene estado local (no se borra nada).",
                externalId
            );
            return;
        }

        // 3) Indexar asientos locales por (fila-columna) para upsert eficiente
        List<Asiento> locales = asientoRepository.findByEventoId(eventoLocal.getId());
        Map<String, Asiento> index = new HashMap<>();
        for (Asiento a : locales) {
            index.put(key(a.getFila(), a.getColumna()), a);
        }

        // 4) Upsert
        AtomicInteger creados = new AtomicInteger(0);
        AtomicInteger actualizados = new AtomicInteger(0);

        for (ProxyAsientoDTO remoto : remotos) {
            String k = key(remoto.getFila(), remoto.getColumna());
            Asiento existente = index.get(k);

            if (existente != null) {
                existente.setEstado(mapearEstado(remoto.getEstado()));
                existente.setPersonaActual(remoto.getPersonaActual());
                asientoRepository.save(existente);
                actualizados.incrementAndGet();
            } else {
                Asiento nuevo = new Asiento()
                    .fila(remoto.getFila())
                    .columna(remoto.getColumna())
                    .estado(mapearEstado(remoto.getEstado()))
                    .personaActual(remoto.getPersonaActual())
                    .evento(eventoLocal);

                asientoRepository.save(nuevo);
                creados.incrementAndGet();
            }
        }

        log.info(
            "✅ [Sync-Asientos] Evento idLocal={} (externalId={}) → Asientos sincronizados: {} creados, {} actualizados.",
            eventoLocal.getId(),
            externalId,
            creados.get(),
            actualizados.get()
        );
    }

    private static String key(Integer fila, Integer columna) {
        return fila + "-" + columna;
    }

    /**
     * Parsea el JSON devuelto por el proxy al tipo {@link ProxyEstadoAsientosResponse}
     * y devuelve la lista de asientos remotos.
     *
     * - Si el JSON está vacío o hay error → lista vacía.
     * - Si el JSON no trae "asientos" o viene null → lista vacía.
     */
    private List<ProxyAsientoDTO> parsearLista(String json) {
        try {
            ProxyEstadoAsientosResponse response = objectMapper.readValue(json, ProxyEstadoAsientosResponse.class);

            if (response == null || response.getAsientos() == null || response.getAsientos().isEmpty()) {
                // No asumir que vacío = "sin asientos"; normalmente significa "no hay estado remoto en Redis"
                log.info("ℹ️  [Sync-Asientos] El proxy no devolvió asientos (response/asientos vacío).");
                return Collections.emptyList();
            }

            return response.getAsientos();
        } catch (Exception e) {
            log.error("💥 [Sync-Asientos] Error procesando JSON de asientos del proxy", e);
            return Collections.emptyList();
        }
    }

    /**
     * Mapea el estado remoto (String) al enum local AsientoEstado.
     * Soporta valores como: "LIBRE", "BLOQUEADO", "VENDIDO", "OCUPADO".
     * Si viene null o un valor inesperado, se usa LIBRE por defecto.
     */
    private AsientoEstado mapearEstado(String estadoRemoto) {
        if (estadoRemoto == null) {
            return AsientoEstado.LIBRE;
        }

        String normalizado = estadoRemoto.trim().toUpperCase();

        switch (normalizado) {
            case "LIBRE":
                return AsientoEstado.LIBRE;
            case "BLOQUEADO":
                return AsientoEstado.BLOQUEADO;
            case "VENDIDO":
            case "OCUPADO":
                return AsientoEstado.VENDIDO;
            default:
                log.warn("⚠️  [Sync-Asientos] Estado remoto desconocido='{}'. Usando LIBRE por defecto.", estadoRemoto);
                return AsientoEstado.LIBRE;
        }
    }
}
