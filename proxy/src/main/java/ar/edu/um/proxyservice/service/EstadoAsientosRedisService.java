package ar.edu.um.proxyservice.service;
import ar.edu.um.proxyservice.service.dto.EstadoAsientosRemotoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Collections;
import org.springframework.data.redis.connection.DataType;
import ar.edu.um.proxyservice.service.dto.AsientoRequestDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Servicio encargado de leer desde el Redis REMOTO de la cátedra el estado actual de los asientos de un evento.
 *
 * Este servicio cumple el rol de integración:
 *  - Obtiene el JSON crudo desde Redis.
 *  - Lo parsea hacia DTOs remotos.
 *  - Maneja casos donde no exista información.
 *  - Es tolerante a errores de formato (no debe romper el proxy).
 *
 * Soporta dos formatos:
 *  - evento_X como STRING: JSON con { "eventoId": ..., "asientos": [ { fila, columna, estado }, ... ] }
 *  - evento_X como HASH: cada campo es un seatId "r2c6" y el valor es un JSON con estado BLOQUEADO/VENDIDO.
 *
 * IMPORTANTE:
 * El proxy es el ÚNICO servicio que puede consultar el Redis de la cátedra.
 * El backend del alumno debe consultar SIEMPRE al proxy, nunca a Redis directo.
 */
@Service
public class EstadoAsientosRedisService {

    /** Template para operar contra el Redis remoto (strings y hashes). */
    private final StringRedisTemplate stringRedisTemplate;

    /** ObjectMapper para convertir JSON <-> DTOs internos del proxy. */
    private final ObjectMapper objectMapper;

    /** Logger específico de este servicio de integración con Redis. */
    private final Logger log = LoggerFactory.getLogger(EstadoAsientosRedisService.class);

    public EstadoAsientosRedisService(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Devuelve una representación "raw" del contenido en Redis para debugging.
     *
     * - Si es STRING → devuelve el JSON tal cual.
     * - Si es HASH   → devuelve el hash serializado como JSON (map field -> value).
     * - Si no existe o hay error → devuelve null.
     */
    public String obtenerEstadoAsientosRaw(Long eventoId) {
        String key = buildKey(eventoId);
        DataType type = stringRedisTemplate.type(key);

        if (type == null || type == DataType.NONE) {
            log.info("[Redis] RAW key={} → NO ENCONTRADO", key);
            return null;
        }

        try {
            if (type == DataType.STRING) {
                String json = stringRedisTemplate.opsForValue().get(key);
                log.info(
                        "[Redis] RAW key={} → STRING (len={})",
                        key,
                        json != null ? json.length() : 0
                );
                return json;
            }

            if (type == DataType.HASH) {
                Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
                log.info(
                        "[Redis] RAW key={} → HASH (campos={})",
                        key,
                        entries.size()
                );
                return objectMapper.writeValueAsString(entries);
            }

            log.warn("[Redis] RAW key={} → tipo inesperado {}", key, type);
            return null;
        } catch (Exception e) {
            log.error("[Redis] RAW Error serializando contenido para key={}", key, e);
            return null;
        }
    }

    /**
     * Obtiene el estado de asientos de un evento desde el Redis remoto, unificando
     * los diferentes formatos posibles (STRING o HASH) en un único DTO:
     * {@link EstadoAsientosRemotoDTO}.
     *
     * Características importantes:
     *  - NUNCA devuelve null.
     *  - Si no hay información o hay errores de parseo, devuelve un DTO con lista vacía.
     *  - Es tolerante a datos inconsistentes: loguea y sigue.
     *
     * @param eventoId id del evento en el sistema de la cátedra (externalId).
     * @return DTO con eventoId y lista de asientos (posiblemente vacía).
     */
    public EstadoAsientosRemotoDTO obtenerEstadoAsientos(Long eventoId) {
        String key = buildKey(eventoId);
        DataType type = stringRedisTemplate.type(key);

        if (type == null || type == DataType.NONE) {
            log.info("[Redis] No hay estado de asientos en Redis para eventoId={} (key {}) type:({}).", eventoId, key, type);
            return dtoVacio(eventoId);
        }

        try {
            if (type == DataType.STRING) {
                return leerDesdeString(eventoId, key);
            }

            if (type == DataType.HASH) {
                return leerDesdeHash(eventoId, key);
            }

            // Cualquier tipo diferente de STRING/HASH se considera inesperado.
            log.warn("⚠️  [Redis] Key={} tiene tipo inesperado {}. Se ignora y se devuelve DTO vacío.", key, type);
            return dtoVacio(eventoId);

        } catch (Exception e) {
            // Nunca dejamos que una excepción de Redis tumbe el proxy.
            log.error("❌ [Redis] Error leyendo estado de asientos desde Redis para eventoId={} y key={}", eventoId, key, e);
            return dtoVacio(eventoId);
        }
    }

    /**
     * Lee el estado de asientos desde Redis cuando la key es un STRING con JSON
     * del estilo:
     *  {
     *    "eventoId": 1,
     *    "asientos": [ { "fila": 1, "columna": 2, "estado": "Vendido" }, ... ]
     *  }
     */
    private EstadoAsientosRemotoDTO leerDesdeString(Long eventoId, String key) {
        String json = stringRedisTemplate.opsForValue().get(key);

        if (json == null) {
            log.info("[Redis] STRING no encontrado para eventoId={} (key {}). Devolviendo DTO vacío.", eventoId, key);
            return dtoVacio(eventoId);
        }

        try {
            EstadoAsientosRemotoDTO dto = objectMapper.readValue(json, EstadoAsientosRemotoDTO.class);

            // Si el JSON no trae eventoId, lo forzamos para que el DTO quede consistente.
            if (dto.getEventoId() == null) {
                dto.setEventoId(eventoId);
            }

            log.info("[Redis] Redis STRING eventoId={} → {} asientos", dto.getEventoId(), dto.getAsientos().size());
            return dto;

        } catch (Exception e) {
            // Error de parseo: se loguea y se entrega un DTO seguro (lista vacía).
            log.error("❌ [Redis] Error parseando STRING Redis key={} para eventoId={}. json={}", key, eventoId, json, e);
            return dtoVacio(eventoId);
        }
    }

    /**
     * Lee el estado de asientos desde Redis cuando la key es un HASH donde:
     *  - Cada field es un seatId del estilo "r2c6".
     *  - Cada valor es un JSON con el estado del asiento.
     */
    private EstadoAsientosRemotoDTO leerDesdeHash(Long eventoId, String key) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);

        if (entries.isEmpty()) {
            log.info("[Redis] Redis HASH vacío para eventoId={} (key {}). Devolviendo DTO vacío.", eventoId, key);
            return dtoVacio(eventoId);
        }

        List<AsientoRequestDTO> asientos = new ArrayList<>();

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            if (!(entry.getValue() instanceof String jsonSeat)) {
                log.warn("⚠️ [Redis] Valor HASH no es String para key={}, field={}. Se ignora.", key, entry.getKey());
                continue;
            }

            try {
                // Mapeamos el JSON del hash a un DTO interno minimalista (seatId + status).
                SeatHashEntry seat = objectMapper.readValue(jsonSeat, SeatHashEntry.class);

                FilaColumna fc = parseSeatId(seat.getSeatId());
                if (fc == null) {
                    log.warn("⚠️ [Redis] seatId con formato inesperado '{}' en key={}, field={}", seat.getSeatId(), key, entry.getKey());
                    continue;
                }

                AsientoRequestDTO dto = new AsientoRequestDTO();
                dto.setFila(fc.fila());
                dto.setColumna(fc.columna());
                dto.setExpira(seat.getExpira());

                // Normalizamos el estado al formato usado por el backend ("Bloqueado"/"Vendido").
                if ("BLOQUEADO".equalsIgnoreCase(seat.getStatus())) {
                    dto.setEstado("Bloqueado");
                } else if ("VENDIDO".equalsIgnoreCase(seat.getStatus())) {
                    dto.setEstado("Vendido");
                } else {
                    dto.setEstado(seat.getStatus());
                }

                asientos.add(dto);

            } catch (Exception e) {
                log.warn(
                        "⚠️ [Redis] Error parseando entrada HASH Redis key={} field={} json={}",
                        key,
                        entry.getKey(),
                        jsonSeat,
                        e
                );
            }
        }

        EstadoAsientosRemotoDTO dto = new EstadoAsientosRemotoDTO();
        dto.setEventoId(eventoId);
        dto.setAsientos(asientos);

        log.info("[Redis] Redis HASH eventoId={} → {} asientos", eventoId, asientos.size());
        return dto;
    }

    // ----------------------------------------------------------------
    // Helpers de creación de DTO y utilitarios
    // ----------------------------------------------------------------

    private EstadoAsientosRemotoDTO dtoVacio(Long eventoId) {
        EstadoAsientosRemotoDTO dto = new EstadoAsientosRemotoDTO();
        dto.setEventoId(eventoId);
        dto.setAsientos(Collections.emptyList());
        return dto;
    }

    private String buildKey(Long eventoId) {
        return "evento_" + eventoId;
    }

    /**
     * Parsea un seatId del estilo "r2c6" en un objeto con fila=2 y columna=6.
     */
    private FilaColumna parseSeatId(String seatId) {
        if (seatId == null || !seatId.startsWith("r")) {
            return null;
        }

        String[] parts = seatId.substring(1).split("c");
        if (parts.length != 2) {
            return null;
        }

        try {
            return new FilaColumna(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1])
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ----------------------------------------------------------------
    // DTO internos de apoyo para parsear el formato HASH de Redis
    // ----------------------------------------------------------------

    private static class SeatHashEntry {
        private String seatId;
        private String status;
        private Instant expira;

        public String getSeatId() {
            return seatId;
        }

        public void setSeatId(String seatId) {
            this.seatId = seatId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Instant getExpira() {
            return expira;
        }

        public void setExpira(Instant expira) {
            this.expira = expira;
        }
    }

    /**
     * Helper inmutable para representar fila/columna ya parseadas desde seatId.
     */
    private record FilaColumna(int fila, int columna) {}
}
