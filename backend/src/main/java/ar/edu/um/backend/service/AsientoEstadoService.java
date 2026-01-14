package ar.edu.um.backend.service;
import ar.edu.um.backend.domain.Evento;
import ar.edu.um.backend.repository.EventoRepository;
import ar.edu.um.backend.service.dto.AsientoEstadoDTO;
import ar.edu.um.backend.service.dto.AsientoRequestDTO;
import ar.edu.um.backend.service.dto.ProxyEstadoAsientosResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Servicio que construye el MAPA FINAL DE ASIENTOS para el frontend usando SOLO Redis (vía proxy).
 *
 * Regla:
 * - Redis devuelve únicamente asientos NO libres (bloqueados/vendidos/ocupados) (ideal).
 * - El backend completa los faltantes como LIBRE generando la grilla completa (filas x columnas).
 *
 * Estados devueltos al frontend:
 * - LIBRE
 * - VENDIDO
 * - BLOQUEADO_VIGENTE
 * - BLOQUEADO_EXPIRADO (si expira <= now)
 */
@Service
public class AsientoEstadoService {

    private static final Logger log = LoggerFactory.getLogger(AsientoEstadoService.class);

    private final EventoRepository eventoRepository;
    private final ProxyService proxyService;

    public AsientoEstadoService(EventoRepository eventoRepository, ProxyService proxyService) {
        this.eventoRepository = eventoRepository;
        this.proxyService = proxyService;
    }

    /**
     * Devuelve el mapa completo de asientos del evento (grilla completa),
     * usando Redis como fuente de verdad y completando LIBRES por diferencia.
     */
    public List<AsientoEstadoDTO> obtenerEstadoActualDeAsientos(Long eventoIdLocal) {

        Evento evento = eventoRepository
            .findById(eventoIdLocal)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));

        if (evento.getExternalId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El evento no tiene externalId (id cátedra)");
        }
        if (evento.getFilaAsientos() == null || evento.getColumnaAsientos() == null
            || evento.getFilaAsientos() <= 0 || evento.getColumnaAsientos() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El evento no tiene filas/columnas válidas");
        }

        int maxFilas = evento.getFilaAsientos();
        int maxCols = evento.getColumnaAsientos();
        Long externalId = evento.getExternalId();
        int totalEsperado = maxFilas * maxCols;

        // 1) Leer Redis (solo no-libres)
        ProxyEstadoAsientosResponse redisResponse = proxyService.listarEstadoAsientosRedis(externalId);
        List<AsientoRequestDTO> redisAsientos =
            (redisResponse != null && redisResponse.getAsientos() != null)
                ? redisResponse.getAsientos()
                : Collections.emptyList();

        log.info(
            "🧭 [Mapa-Asientos] Construyendo mapa eventoIdLocal={} externalId={} grilla={}x{} (total={}) | redisItems={}",
            eventoIdLocal, externalId, maxFilas, maxCols, totalEsperado, redisAsientos.size()
        );

        // 2) Indexar Redis (filtrando fuera de rango y opcionalmente ignorando LIBRE)
        Map<String, AsientoRequestDTO> redisMap = new HashMap<>();
        int ignoradosFueraDeRango = 0;
        int ignoradosLibres = 0;
        int ignoradosInvalidos = 0;
        int duplicados = 0;

        for (AsientoRequestDTO a : redisAsientos) {
            if (a == null || a.getFila() == null || a.getColumna() == null) {
                ignoradosInvalidos++;
                continue;
            }
            int fila = a.getFila();
            int col = a.getColumna();

            boolean fueraDeRango = fila < 1 || col < 1 || fila > maxFilas || col > maxCols;
            if (fueraDeRango) {
                ignoradosFueraDeRango++;
                continue;
            }

            String estadoNorm = normalizarEstadoRedis(a.getEstado());

            // Si Redis manda "LIBRE", lo ignoramos porque el default ya es LIBRE
            if ("LIBRE".equals(estadoNorm)) {
                ignoradosLibres++;
                continue;
            }

            String k = key(fila, col);
            if (redisMap.containsKey(k)) {
                duplicados++;
            }
            redisMap.put(k, a);
        }

        if (ignoradosFueraDeRango > 0 || ignoradosLibres > 0 || ignoradosInvalidos > 0 || duplicados > 0) {
            log.warn(
                "⚠️ [Mapa-Asientos] Redis filtrado eventoIdLocal={} externalId={} -> usados={} | invalidos={} | fueraDeRango={} | libresIgnorados={} | duplicados={}",
                eventoIdLocal, externalId, redisMap.size(), ignoradosInvalidos, ignoradosFueraDeRango, ignoradosLibres, duplicados
            );
        } else {
            log.info(
                "✅ [Mapa-Asientos] Redis indexado eventoIdLocal={} externalId={} -> usados={} (no-libres)",
                eventoIdLocal, externalId, redisMap.size()
            );
        }

        // 3) Construir grilla completa
        List<AsientoEstadoDTO> resultado = new ArrayList<>(totalEsperado);
        Instant ahora = Instant.now();

        for (int fila = 1; fila <= maxFilas; fila++) {
            for (int col = 1; col <= maxCols; col++) {

                AsientoRequestDTO redis = redisMap.get(key(fila, col));
                if (redis == null) {
                    resultado.add(new AsientoEstadoDTO(fila, col, "LIBRE", null));
                    continue;
                }

                String estadoRedis = normalizarEstadoRedis(redis.getEstado());
                Instant expira = redis.getExpira();

                if ("VENDIDO".equals(estadoRedis) || "OCUPADO".equals(estadoRedis)) {
                    resultado.add(new AsientoEstadoDTO(fila, col, "VENDIDO", null));
                    continue;
                }

                if ("BLOQUEADO".equals(estadoRedis)) {
                    if (expira != null && expira.isAfter(ahora)) {
                        resultado.add(new AsientoEstadoDTO(fila, col, "BLOQUEADO_VIGENTE", expira));
                    } else if (expira != null) {
                        resultado.add(new AsientoEstadoDTO(fila, col, "BLOQUEADO_EXPIRADO", expira));
                    } else {
                        // Bloqueado sin expira explícita
                        resultado.add(new AsientoEstadoDTO(fila, col, "BLOQUEADO_VIGENTE", null));
                    }
                    continue;
                }

                // Cualquier otro estado raro: lo devolvemos tal cual normalizado
                resultado.add(new AsientoEstadoDTO(fila, col, estadoRedis, expira));
            }
        }

        return resultado;
    }

    /**
     * Estado de un asiento puntual.
     * (NO reconstruye toda la grilla).
     *
     * Regla:
     * - Si no aparece en Redis => LIBRE.
     */
    public AsientoEstadoDTO obtenerEstadoAsiento(Long eventoIdLocal, int fila, int columna) {

        Evento evento = eventoRepository
            .findById(eventoIdLocal)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));

        if (evento.getExternalId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El evento no tiene externalId (id cátedra)");
        }

        int maxFilas = Optional.ofNullable(evento.getFilaAsientos()).orElse(0);
        int maxCols = Optional.ofNullable(evento.getColumnaAsientos()).orElse(0);

        if (fila < 1 || columna < 1 || fila > maxFilas || columna > maxCols) {
            // OJO: normalmente esto NO debería pasar porque Bloqueo/Venta validan rango antes.
            log.warn(
                "⚠️ [Mapa-Asientos] obtenerEstadoAsiento fuera de rango eventoIdLocal={} externalId={} seat=({},{}) rango=1-{}x1-{}",
                eventoIdLocal, evento.getExternalId(), fila, columna, maxFilas, maxCols
            );
            return null;
        }

        ProxyEstadoAsientosResponse redisResponse = proxyService.listarEstadoAsientosRedis(evento.getExternalId());
        List<AsientoRequestDTO> redisAsientos =
            (redisResponse != null && redisResponse.getAsientos() != null)
                ? redisResponse.getAsientos()
                : Collections.emptyList();

        Instant ahora = Instant.now();

        for (AsientoRequestDTO a : redisAsientos) {
            if (a == null || a.getFila() == null || a.getColumna() == null) continue;
            if (a.getFila() != fila || a.getColumna() != columna) continue;

            String estadoRedis = normalizarEstadoRedis(a.getEstado());
            Instant expira = a.getExpira();

            if ("LIBRE".equals(estadoRedis)) {
                return new AsientoEstadoDTO(fila, columna, "LIBRE", null);
            }
            if ("VENDIDO".equals(estadoRedis) || "OCUPADO".equals(estadoRedis)) {
                return new AsientoEstadoDTO(fila, columna, "VENDIDO", null);
            }
            if ("BLOQUEADO".equals(estadoRedis)) {
                if (expira != null && expira.isAfter(ahora)) {
                    return new AsientoEstadoDTO(fila, columna, "BLOQUEADO_VIGENTE", expira);
                } else if (expira != null) {
                    return new AsientoEstadoDTO(fila, columna, "BLOQUEADO_EXPIRADO", expira);
                } else {
                    return new AsientoEstadoDTO(fila, columna, "BLOQUEADO_VIGENTE", null);
                }
            }

            return new AsientoEstadoDTO(fila, columna, estadoRedis, expira);
        }

        // Si no está en Redis => es LIBRE por definición del modelo
        log.debug(
            "ℹ️ [Mapa-Asientos] Asiento no presente en Redis => LIBRE eventoIdLocal={} externalId={} seat=({},{})",
            eventoIdLocal, evento.getExternalId(), fila, columna
        );
        return new AsientoEstadoDTO(fila, columna, "LIBRE", null);
    }

    private static String key(int fila, int columna) {
        return fila + "-" + columna;
    }

    /**
     * Normaliza estados devueltos por Redis/cátedra.
     * Acepta: Bloqueado/BLOQUEADO, Vendido/VENDIDO, Ocupado/OCUPADO, Libre/LIBRE, etc.
     */
    private String normalizarEstadoRedis(String estado) {
        if (estado == null) return "LIBRE";

        String e = estado.trim().toUpperCase(Locale.ROOT);

        if (e.contains("BLOQ")) return "BLOQUEADO";
        if (e.contains("VEND")) return "VENDIDO";
        if (e.contains("OCUP")) return "OCUPADO";
        if (e.contains("LIBR")) return "LIBRE";

        return e;
    }
}

