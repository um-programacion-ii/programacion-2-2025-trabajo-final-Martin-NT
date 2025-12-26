package ar.edu.um.backend.service;
import ar.edu.um.backend.domain.Asiento;
import ar.edu.um.backend.domain.Evento;
import ar.edu.um.backend.domain.enumeration.AsientoEstado;
import ar.edu.um.backend.repository.AsientoRepository;
import ar.edu.um.backend.service.dto.AsientoRequestDTO;
import ar.edu.um.backend.service.dto.ProxyEstadoAsientosResponse;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
/**
 * Sincroniza los asientos locales de un evento con los datos remotos del proxy/cátedra.
 *
 * Objetivo REAL para tu arquitectura actual:
 * - La FUENTE DE VERDAD del estado es Redis (vía proxy).
 * - La DB local se usa para:
 *    1) tener la grilla completa (filas*cols) como entidades persistidas (FK con ventas),
 *    2) guardar ventas locales y mantener integridad referencial.
 *
 * Regla acordada:
 * - Si el proxy/Redis devuelve solo NO-LIBRES, entonces "faltantes" => LIBRE (en tiempo real).
 *
 * Estrategia (optimizada):
 * 1) Asegurar grilla completa en DB (crear los que falten como LIBRE).
 * 2) Aplicar estados remotos SOLO para los asientos presentes en la respuesta.
 * 3) NO recorrer la grilla completa para “liberar por diferencia”.

 *   El mapa final para el frontend lo calcula AsientoEstadoService contra Redis (y completa LIBRES).
 *   Por performance y coherencia con la fuente de verdad, evitamos “liberar por diferencia” en DB.
 *
 * Importante:
 * - NO se borran asientos locales (evita romper FKs con ventas).
 * - Se validan coordenadas remotas (fuera de rango se ignoran).
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
                "⚠️ [Sync-Asientos] Evento idLocal={} externalId={} sin filas/cols válidas (filas={}, cols={}). Se omite sync.",
                eventoLocal.getId(),
                externalId,
                maxFilas,
                maxCols
            );
            return;
        }

        int totalEsperado = maxFilas * maxCols;

        log.info(
            "🔄 [Sync-Asientos] Iniciando sync evento idLocal={} externalId={} grilla={}x{} (totalEsperado={})",
            eventoLocal.getId(),
            externalId,
            maxFilas,
            maxCols,
            totalEsperado
        );

        // 1) Leer remoto (proxy/cátedra)
        ProxyEstadoAsientosResponse response = proxyService.listarAsientosDeEvento(externalId);

        List<AsientoRequestDTO> remotos =
            (response != null && response.getAsientos() != null) ? response.getAsientos() : List.of();

        log.info(
            "📥 [Sync-Asientos] Remotos recibidos externalId={} -> {} asiento(s).",
            externalId,
            remotos.size()
        );

        // 2) Cargar locales e indexarlos
        List<Asiento> locales = asientoRepository.findByEventoId(eventoLocal.getId());
        Map<String, Asiento> index = new HashMap<>(Math.max(locales.size() * 2, 16));

        int localesInvalidos = 0;
        for (Asiento a : locales) {
            if (a == null || a.getFila() == null || a.getColumna() == null) {
                localesInvalidos++;
                continue;
            }
            index.put(key(a.getFila(), a.getColumna()), a);
        }
        if (localesInvalidos > 0) {
            log.warn(
                "⚠️ [Sync-Asientos] Se encontraron {} asientos locales inválidos (fila/col null). Fueron ignorados.",
                localesInvalidos
            );
        }

        // 3) Asegurar grilla completa en DB (crear faltantes como LIBRE)
        AtomicInteger creadosGrilla = new AtomicInteger(0);
        List<Asiento> aCrear = new ArrayList<>();

        for (int fila = 1; fila <= maxFilas; fila++) {
            for (int col = 1; col <= maxCols; col++) {
                String k = key(fila, col);
                if (!index.containsKey(k)) {
                    Asiento nuevo = new Asiento()
                        .fila(fila)
                        .columna(col)
                        .estado(AsientoEstado.LIBRE)
                        .personaActual(null)
                        .evento(eventoLocal);

                    aCrear.add(nuevo);
                    index.put(k, nuevo);
                    creadosGrilla.incrementAndGet();
                }
            }
        }

        if (!aCrear.isEmpty()) {
            asientoRepository.saveAll(aCrear);
        }

        if (creadosGrilla.get() > 0) {
            log.info(
                "🧩 [Sync-Asientos] Grilla completada: {} asiento(s) creados para llegar a totalEsperado={}",
                creadosGrilla.get(),
                totalEsperado
            );
        }

        // 4) Aplicar estados remotos SOLO para los asientos presentes
        AtomicInteger actualizadosPorRemoto = new AtomicInteger(0);
        AtomicInteger ignorados = new AtomicInteger(0);

        // (Lo dejamos por trazabilidad, aunque en la opción A no se usa para liberar)
        Set<String> keysVistasEnRemoto = new HashSet<>(Math.max(remotos.size() * 2, 16));

        // Para evitar duplicados en saveAll (misma entidad tocada 2 veces), usamos Set
        Set<Asiento> aGuardarCambios = new HashSet<>();

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
                    "⚠️ [Sync-Asientos] Remoto fuera de rango externalId={} ({},{}) rango=1-{} x 1-{} -> IGNORADO",
                    externalId,
                    fila,
                    col,
                    maxFilas,
                    maxCols
                );
                continue;
            }

            String k = key(fila, col);
            keysVistasEnRemoto.add(k);

            Asiento asiento = index.get(k);
            if (asiento == null) {
                // No debería ocurrir porque completamos grilla, pero por las dudas.
                ignorados.incrementAndGet();
                log.warn(
                    "⚠️ [Sync-Asientos] Inconsistencia: no existe en índice el asiento {} para externalId={}",
                    k,
                    externalId
                );
                continue;
            }

            AsientoEstado estadoNuevo = mapearEstado(remoto.getEstado());
            String personaNueva = remoto.getPersonaActual();

            boolean cambio =
                asiento.getEstado() != estadoNuevo ||
                    (asiento.getPersonaActual() == null ? personaNueva != null : !asiento.getPersonaActual().equals(personaNueva));

            if (cambio) {
                asiento.setEstado(estadoNuevo);
                asiento.setPersonaActual(personaNueva);
                aGuardarCambios.add(asiento);
                actualizadosPorRemoto.incrementAndGet();
            }
        }

        if (!aGuardarCambios.isEmpty()) {
            asientoRepository.saveAll(aGuardarCambios);
        }

        log.info(
            "✅ [Sync-Asientos] Fin sync evento idLocal={} externalId={} | totalEsperado={} | remotos={} | creadosGrilla={} | actualizadosRemoto={} | ignorados={}",
            eventoLocal.getId(),
            externalId,
            totalEsperado,
            remotos.size(),
            creadosGrilla.get(),
            actualizadosPorRemoto.get(),
            ignorados.get()
        );
    }

    private static String key(Integer fila, Integer columna) {
        return fila + "-" + columna;
    }

    /**
     * Mapea estado remoto (String) al enum local.
     *
     * Remoto puede venir como: "Bloqueado", "BLOQUEADO", "BLOQUEADO_VIGENTE",
     * "Vendido", "Ocupado", etc.
     */
    private AsientoEstado mapearEstado(String estadoRemoto) {
        String norm = normalizarEstadoRemoto(estadoRemoto);

        return switch (norm) {
            case "LIBRE" -> AsientoEstado.LIBRE;
            case "BLOQUEADO" -> AsientoEstado.BLOQUEADO;
            case "VENDIDO", "OCUPADO" -> AsientoEstado.VENDIDO;
            default -> {
                log.warn("⚠️ [Sync-Asientos] Estado remoto desconocido='{}'. Usando LIBRE por defecto.", estadoRemoto);
                yield AsientoEstado.LIBRE;
            }
        };
    }

    private String normalizarEstadoRemoto(String estado) {
        if (estado == null) return "LIBRE";

        String e = estado.trim().toUpperCase();

        if (e.contains("BLOQ")) return "BLOQUEADO";
        if (e.contains("VEND")) return "VENDIDO";
        if (e.contains("OCUP")) return "OCUPADO";
        if (e.contains("LIBR")) return "LIBRE";

        return e;
    }
}
