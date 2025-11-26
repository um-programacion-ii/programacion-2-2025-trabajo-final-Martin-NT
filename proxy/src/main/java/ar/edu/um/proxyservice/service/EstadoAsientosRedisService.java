package ar.edu.um.proxyservice.service;
import ar.edu.um.proxyservice.service.dto.EstadoAsientosRemotoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;

/**
 * Servicio encargado de leer desde el Redis REMOTO de la cátedra el estado actual de los asientos de un evento.
 *
 * Este servicio cumple el rol de integración:
 *  - Obtiene el JSON crudo desde Redis.
 *  - Lo parsea hacia DTOs remotos.
 *  - Maneja casos donde no exista información.
 *  - Es tolerante a errores de formato (no debe romper el proxy).
 *
 * IMPORTANTE:
 * El proxy es el ÚNICO servicio que puede consultar el Redis de la cátedra.
 * El backend del alumno debe consultar SIEMPRE al proxy, nunca a Redis directo.
 */
@Service
public class EstadoAsientosRedisService {

    /** Template que permite leer valores String desde Redis (opsForValue().get). */
    private final StringRedisTemplate stringRedisTemplate;

    /** ObjectMapper para convertir JSON -> DTOs. */
    private final ObjectMapper objectMapper;

    /** Logger para debugging e información de integración. */
    private final Logger log = LoggerFactory.getLogger(EstadoAsientosRedisService.class);

    // Constructor
    public EstadoAsientosRedisService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Retorna el JSON crudo almacenado en Redis para un evento.
     * Ejemplo: key = "evento_5"
     * Se usa para logs y debugging.
     */
    public String obtenerEstadoAsientosRaw(Long eventoId) {
        String key = buildKey(eventoId);

        // Consulta directa a Redis usando StringRedisTemplate
        String json = stringRedisTemplate.opsForValue().get(key);

        log.info("Consultando Redis para key={}, resultado={}", key,
                json != null ? "ENCONTRADO" : "NO ENCONTRADO");

        return json;
    }

    /**
     * Obtiene el estado de asientos de un evento, ya parseado en un DTO.
     *
     * Flujo:
     * 1. Leer JSON crudo desde Redis.
     * 2. Si la key no existe -> devolver DTO vacío (lista vacía).
     * 3. Intentar parsear JSON a EstadoAsientosRemotoDTO.
     * 4. Si ocurre un error de parseo -> loguear y devolver DTO vacío.
     *
     * Este metodo NUNCA debe lanzar excepciones hacia afuera:
     * el proxy no puede caerse porque Redis tenga datos inconsistentes.
     */
    public EstadoAsientosRemotoDTO obtenerEstadoAsientos(Long eventoId) {
        String json = obtenerEstadoAsientosRaw(eventoId);
        String key = buildKey(eventoId);

        // Caso: no existe información en Redis
        if (json == null) {
            log.info("No hay asientos bloqueados/vendidos para eventoId={} (key {}). Devolviendo lista vacía.", eventoId, key);
            EstadoAsientosRemotoDTO dto = new EstadoAsientosRemotoDTO();
            dto.setEventoId(eventoId);
            dto.setAsientos(Collections.emptyList());
            return dto;
        }

        // Intentar parseo del JSON remoto
        try {
            EstadoAsientosRemotoDTO dto =
                    objectMapper.readValue(json, EstadoAsientosRemotoDTO.class);

            // Si el JSON remoto no incluye eventoId, lo forzamos.
            if (dto.getEventoId() == null) {
                dto.setEventoId(eventoId);
            }

            log.info("Se parseó correctamente estado de asientos para eventoId={} ({} asientos).",
                    dto.getEventoId(), dto.getAsientos().size());

            return dto;

        } catch (Exception e) {
            // Error de parseo → devolver DTO seguro (no romper el proxy)
            log.error("Error parseando JSON de Redis para eventoId={} y key {}. json={}", eventoId, key, json, e);

            EstadoAsientosRemotoDTO dto = new EstadoAsientosRemotoDTO();
            dto.setEventoId(eventoId);
            dto.setAsientos(Collections.emptyList());
            return dto;
        }
    }

    /**
     * Construye el nombre de la key usada en Redis para guardar asientos de un evento.
     * Ejemplo: evento_1, evento_42, etc.
     */
    private String buildKey(Long eventoId) {
        return "evento_" + eventoId;
    }
}
