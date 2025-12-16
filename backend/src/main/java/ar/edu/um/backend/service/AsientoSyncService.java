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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
/**
 * Servicio encargado de sincronizar los asientos locales de un evento
 * con la información real proveniente de la cátedra (vía proxy).
 *
 * Estrategia:
 *   1. Se obtienen los asientos reales desde el proxy (Redis remoto).
 *   2. Se eliminan todos los asientos locales del evento.
 *   3. Se recrean los asientos según el mapa remoto (solo bloqueados/vendidos).
 *
 * Esto garantiza que, si cambia la capacidad (filas/columnas),
 * el mapa local se regenera completamente a partir de la fuente de verdad.
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
     * Sincroniza TODOS los asientos de un evento concreto desde el proxy.
     *
     * Flujo:
     *  - Llama al proxy para obtener el JSON de asientos (bloqueados/vendidos) del evento.
     *  - Parsea el JSON a una lista de {@link ProxyAsientoDTO}.
     *  - Borra todos los asientos locales de ese evento.
     *  - Vuelve a crear los asientos locales a partir de la lista remota.
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

        // 1. Obtener JSON crudo desde el proxy
        String json = proxyService.listarAsientosDeEvento(externalId);

        if (json == null) {
            log.warn(
                "⚠️  [Sync-Asientos] No se pudo obtener la lista de asientos desde el proxy para externalId={}. Se mantiene estado local.",
                externalId
            );
            return;
        }

        // 2. Convertir JSON → lista de DTOs de asientos remotos
        List<ProxyAsientoDTO> remotos = parsearLista(json);

        if (remotos.isEmpty()) {
            log.warn(
                "⚠️  [Sync-Asientos] Lista de asientos vacía para externalId={}. Se eliminarán asientos locales del evento.",
                externalId
            );
        }

        // 3. Eliminar todos los asientos locales del evento (estrategia de regeneración completa)
        long eliminados = asientoRepository.deleteByEventoId(eventoLocal.getId());
        log.info(
            "🧹 [Sync-Asientos] Asientos previos eliminados para evento idLocal={} → {} asiento(s) borrado(s).",
            eventoLocal.getId(),
            eliminados
        );

        // 4. Crear nuevamente los asientos según los datos remotos
        AtomicInteger creados = new AtomicInteger(0);
        AtomicInteger actualizados = new AtomicInteger(0); // por ahora siempre 0 (regeneración completa)

        for (ProxyAsientoDTO remoto : remotos) {
            Asiento asiento = new Asiento()
                .fila(remoto.getFila())
                .columna(remoto.getColumna())
                .estado(mapearEstado(remoto.getEstado()))
                .personaActual(remoto.getPersonaActual())
                .evento(eventoLocal);

            asientoRepository.save(asiento);
            creados.incrementAndGet();
        }

        log.info(
            "✅ [Sync-Asientos] Evento idLocal={} (externalId={}) → Asientos sincronizados: {} creados, {} actualizados.",
            eventoLocal.getId(),
            externalId,
            creados.get(),
            actualizados.get()
        );
    }

    /**
     * Parsea el JSON devuelto por el proxy al tipo {@link ProxyEstadoAsientosResponse}
     * y devuelve la lista de asientos remotos.
     *
     * - Si el JSON está vacío o hay error → lista vacía.
     * - Si el JSON no trae "asientos" o viene null → lista vacía.
     *
     * @param json String JSON proveniente del proxy.
     * @return Lista de asientos remotos (puede ser vacía pero nunca null).
     */
    private List<ProxyAsientoDTO> parsearLista(String json) {
        try {
            ProxyEstadoAsientosResponse response =
                objectMapper.readValue(json, ProxyEstadoAsientosResponse.class);

            if (response.getAsientos() == null || response.getAsientos().isEmpty()) {
                log.info("ℹ️  [Sync-Asientos] El proxy devolvió lista vacía de asientos para eventoId={}",
                    response.getEventoId());
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
            case "OCUPADO": // por si la cátedra manda "OCUPADO"
                return AsientoEstado.VENDIDO;
            default:
                log.warn(
                    "⚠️  [Sync-Asientos] Estado remoto desconocido='{}'. Usando LIBRE por defecto.",
                    estadoRemoto
                );
                return AsientoEstado.LIBRE;
        }
    }

}
