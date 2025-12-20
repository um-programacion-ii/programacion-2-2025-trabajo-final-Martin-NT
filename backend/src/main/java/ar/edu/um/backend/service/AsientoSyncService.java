package ar.edu.um.backend.service;
import ar.edu.um.backend.domain.Asiento;
import ar.edu.um.backend.domain.Evento;
import ar.edu.um.backend.domain.enumeration.AsientoEstado;
import ar.edu.um.backend.repository.AsientoRepository;
import ar.edu.um.backend.service.dto.AsientoRequestDTO;
import ar.edu.um.backend.service.dto.ProxyEstadoAsientosResponse;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
/**
 * Sincroniza los asientos locales de un evento con los datos remotos del proxy/cátedra.
 *
 * Estrategia:
 * - NO borra asientos locales (evita romper FKs con ventas).
 * - Hace UPSERT por (fila, columna):
 *    - si existe → actualiza estado/personaActual
 *    - si no existe → crea
 *
 * Importante:
 * - Si el proxy devuelve vacío / null, NO se asume "sin asientos": se mantiene estado local.
 * - Se validan coordenadas remotas para no persistir basura fuera de rango.
 */
@Service
@Transactional
public class AsientoSyncService {

    private static final Logger log = LoggerFactory.getLogger(AsientoSyncService.class);

    private final ProxyService proxyService;
    private final AsientoRepository asientoRepository;

    public AsientoSyncService(ProxyService proxyService, AsientoRepository asientoRepository) {
        this.proxyService = proxyService;
        this.asientoRepository = asientoRepository;
    }

    public void sincronizarAsientosDeEvento(Evento eventoLocal, Long externalId) {
        if (eventoLocal == null || eventoLocal.getId() == null) {
            throw new IllegalStateException("eventoLocal requerido y persistido.");
        }
        if (externalId == null) {
            throw new IllegalStateException("externalId requerido.");
        }

        Integer maxFilas = eventoLocal.getFilaAsientos();
        Integer maxCols = eventoLocal.getColumnaAsientos();

        if (maxFilas == null || maxCols == null || maxFilas <= 0 || maxCols <= 0) {
            log.warn(
                "⚠️ [Sync-Asientos] Evento idLocal={} sin filas/cols válidas (filas={}, cols={}). Se omite sync.",
                eventoLocal.getId(),
                maxFilas,
                maxCols
            );
            return;
        }

        log.info(
            "🔄 [Sync-Asientos] Sync asientos evento idLocal={} (externalId={}) rangoFilas=1-{} rangoCols=1-{}",
            eventoLocal.getId(),
            externalId,
            maxFilas,
            maxCols
        );

        // 1) Obtener respuesta tipada desde el proxy
        ProxyEstadoAsientosResponse response = proxyService.listarAsientosDeEvento(externalId);

        if (response == null || response.getAsientos() == null || response.getAsientos().isEmpty()) {
            log.warn(
                "⚠️ [Sync-Asientos] Proxy devolvió vacío para externalId={}. Se mantiene estado local (no se borra nada).",
                externalId
            );
            return;
        }

        List<AsientoRequestDTO> remotos = response.getAsientos();

        // 2) Indexar asientos locales por (fila-columna)
        List<Asiento> locales = asientoRepository.findByEventoId(eventoLocal.getId());
        Map<String, Asiento> index = new HashMap<>();
        for (Asiento a : locales) {
            index.put(key(a.getFila(), a.getColumna()), a);
        }

        // 3) Upsert con validación de rango
        AtomicInteger creados = new AtomicInteger(0);
        AtomicInteger actualizados = new AtomicInteger(0);
        AtomicInteger ignorados = new AtomicInteger(0);

        for (AsientoRequestDTO remoto : remotos) {
            if (remoto == null || remoto.getFila() == null || remoto.getColumna() == null) {
                ignorados.incrementAndGet();
                continue;
            }

            int fila = remoto.getFila();
            int col = remoto.getColumna();

            boolean fueraDeRango = fila < 1 || col < 1 || fila > maxFilas || col > maxCols;
            if (fueraDeRango) {
                ignorados.incrementAndGet();
                log.warn(
                    "⚠️ [Sync-Asientos] Asiento remoto fuera de rango externalId={} ({},{}) rango=1-{} x 1-{} -> IGNORADO",
                    externalId,
                    fila,
                    col,
                    maxFilas,
                    maxCols
                );
                continue;
            }

            String k = key(fila, col);
            Asiento existente = index.get(k);

            AsientoEstado estadoLocal = mapearEstado(remoto.getEstado());
            String persona = remoto.getPersonaActual();

            if (existente != null) {
                existente.setEstado(estadoLocal);
                existente.setPersonaActual(persona);
                asientoRepository.save(existente);
                actualizados.incrementAndGet();
            } else {
                Asiento nuevo = new Asiento()
                    .fila(fila)
                    .columna(col)
                    .estado(estadoLocal)
                    .personaActual(persona)
                    .evento(eventoLocal);

                asientoRepository.save(nuevo);
                creados.incrementAndGet();
            }
        }

        log.info(
            "✅ [Sync-Asientos] Evento idLocal={} (externalId={}) -> {} creados, {} actualizados, {} ignorados.",
            eventoLocal.getId(),
            externalId,
            creados.get(),
            actualizados.get(),
            ignorados.get()
        );
    }

    private static String key(Integer fila, Integer columna) {
        return fila + "-" + columna;
    }

    /**
     * Mapea estado remoto (String) al enum local.
     * Soporta: LIBRE, BLOQUEADO, VENDIDO, OCUPADO.
     */
    private AsientoEstado mapearEstado(String estadoRemoto) {
        if (estadoRemoto == null) {
            return AsientoEstado.LIBRE;
        }

        String normalizado = estadoRemoto.trim().toUpperCase();

        return switch (normalizado) {
            case "LIBRE" -> AsientoEstado.LIBRE;
            case "BLOQUEADO" -> AsientoEstado.BLOQUEADO;
            case "VENDIDO", "OCUPADO" -> AsientoEstado.VENDIDO;
            default -> {
                log.warn("⚠️ [Sync-Asientos] Estado remoto desconocido='{}'. Usando LIBRE por defecto.", estadoRemoto);
                yield AsientoEstado.LIBRE;
            }
        };
    }
}
