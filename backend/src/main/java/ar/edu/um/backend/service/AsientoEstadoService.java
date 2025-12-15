package ar.edu.um.backend.service;
import ar.edu.um.backend.domain.Asiento;
import ar.edu.um.backend.domain.Evento;
import ar.edu.um.backend.domain.enumeration.AsientoEstado;
import ar.edu.um.backend.repository.AsientoRepository;
import ar.edu.um.backend.repository.EventoRepository;
import ar.edu.um.backend.service.dto.AsientoEstadoDTO;
import ar.edu.um.backend.service.dto.ProxyAsientoDTO;
import ar.edu.um.backend.service.dto.ProxyEstadoAsientosResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Servicio encargado de construir el estado actual de los asientos de un evento,
 * combinando:
 *
 * - El estado persistido en la base local (PostgreSQL, entidad Asiento).
 * - El estado en tiempo real de los bloqueos desde Redis (vía ProxyService).
 *
 * Resultado:
 *   Devuelve una lista de AsientoEstadoDTO para que el frontend tenga
 *   un mapa unificado: LIBRE / VENDIDO / BLOQUEADO_VIGENTE / BLOQUEADO_EXPIRADO.
 */
@Service
public class AsientoEstadoService {
    private static final Logger log = LoggerFactory.getLogger(AsientoEstadoService.class);
    private final EventoRepository eventoRepository;
    private final AsientoRepository asientoRepository;
    private final ProxyService proxyService;
    private final ObjectMapper objectMapper;

    public AsientoEstadoService(
        EventoRepository eventoRepository,
        AsientoRepository asientoRepository,
        ProxyService proxyService,
        ObjectMapper objectMapper
    ) {
        this.eventoRepository = eventoRepository;
        this.asientoRepository = asientoRepository;
        this.proxyService = proxyService;
        this.objectMapper = objectMapper;
    }

    /**
     * Obtiene el mapa FINAL de asientos de un evento combinando:
     * - Lo que hay en la base local (tabla asiento).
     * - El estado actual de bloqueos desde Redis.
     *
     * Reglas principales:
     *  - Si el asiento está VENDIDO en DB → tiene prioridad sobre Redis.
     *  - Redis decide si un bloqueo está vigente o expirado (usando el campo expira).
     *  - Coordenadas inválidas devueltas por Redis se ignoran pero se loguean.
     *
     * @param eventoId ID local del evento (en nuestra base).
     * @return lista de AsientoEstadoDTO con el estado final de cada asiento.
     */
    public List<AsientoEstadoDTO> obtenerEstadoActualDeAsientos(Long eventoId) {
        // 1) Obtener el evento para conocer filas/columnas y el externalId
        Evento evento = eventoRepository.findById(eventoId)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Evento no encontrado"
                )
            );

        // 2) Asientos desde la DB local (mapa base)
        List<Asiento> asientosDB =
            asientoRepository.findByEventoIdOrderByFilaAscColumnaAsc(eventoId);

        // 3) Estado de asientos desde Redis vía proxy
        String jsonRedis = proxyService.listarEstadoAsientosRedis(evento.getExternalId());
        ProxyEstadoAsientosResponse redisResponse = parsearRedis(jsonRedis);

        List<ProxyAsientoDTO> redisAsientos =
            (redisResponse != null && redisResponse.getAsientos() != null)
                ? redisResponse.getAsientos()
                : Collections.emptyList();

        // 4) Armar mapa rápido (fila-columna → asientoRedis)
        Map<String, ProxyAsientoDTO> redisMap = new HashMap<>();
        for (ProxyAsientoDTO a : redisAsientos) {
            String key = a.getFila() + "-" + a.getColumna();
            redisMap.put(key, a);
        }

        // 5) Recorrer todos los asientos de la DB y decidir estado final
        List<AsientoEstadoDTO> resultado = new ArrayList<>();
        Instant ahora = Instant.now();

        for (Asiento asientoDB : asientosDB) {

            String key = asientoDB.getFila() + "-" + asientoDB.getColumna();
            ProxyAsientoDTO redis = redisMap.get(key);

            // ----------- VALIDACIONES DE REDIS ----------- //
            if (redis != null) {

                boolean invalido =
                    // 1) Redis devolvió fila o columna sin valor (null) → JSON incompleto o corrupto
                    redis.getFila() == null || redis.getColumna() == null ||

                    // 2) La fila o columna es 0 o negativa → nunca puede existir un asiento así
                    redis.getFila() <= 0 || redis.getColumna() <= 0 ||

                    // 3) La fila supera la cantidad de filas declaradas por el evento → fuera del rango real del evento
                    redis.getFila() > evento.getFilaAsientos() ||

                    // 4) La columna supera la cantidad de columnas del evento → también fuera del rango
                    redis.getColumna() > evento.getColumnaAsientos();

                if (invalido) {
                    // Se imprime cuando cualquiera de las validaciones anteriores falla
                    log.warn(
                        "⚠️  [Redis] Asiento remoto inválido ({}, {}): fuera de rango para evento idLocal={} (filas 1-{}, columnas 1-{})",
                        redis.getFila(),
                        redis.getColumna(),
                        evento.getId(),
                        evento.getFilaAsientos(),
                        evento.getColumnaAsientos()
                    );

                    // Si es inválido → se descarta el dato de Redis
                    redis = null;
                }
            }


            // ----------- REGLA 1: VENDIDO EN DB MANDA ----------- //
            if (asientoDB.getEstado() == AsientoEstado.VENDIDO) {
                resultado.add(
                    new AsientoEstadoDTO(asientoDB.getFila(), asientoDB.getColumna(), "VENDIDO", null)
                );
                continue;
            }

            // ----------- REGLA 2: REDIS PUEDE INDICAR BLOQUEO ----------- //
            if (redis != null && redis.getExpira() != null) {
                Instant expira = redis.getExpira();

                if (expira.isAfter(ahora)) {
                    // Bloqueo vigente
                    log.info(
                        "🔒 [Redis] Asiento ({},{}) bloqueado vigente",
                        asientoDB.getFila(),
                        asientoDB.getColumna()
                    );

                    resultado.add(
                        new AsientoEstadoDTO(
                            asientoDB.getFila(),
                            asientoDB.getColumna(),
                            "BLOQUEADO_VIGENTE",
                            expira
                        )
                    );
                } else {
                    // Bloqueo expirado → lo marcamos como BLOQUEADO_EXPIRADO
                    log.info(
                        "🕒 [Redis] Asiento ({},{}) bloqueo expirado",
                        asientoDB.getFila(),
                        asientoDB.getColumna()
                    );

                    resultado.add(
                        new AsientoEstadoDTO(
                            asientoDB.getFila(),
                            asientoDB.getColumna(),
                            "BLOQUEADO_EXPIRADO",
                            expira
                        )
                    );
                }

                continue; // ya decidimos por Redis para este asiento
            }

            // ----------- REGLA 3: SI NO HAY NADA ESPECIAL → LIBRE ----------- //
            resultado.add(
                new AsientoEstadoDTO(
                    asientoDB.getFila(),
                    asientoDB.getColumna(),
                    "LIBRE",
                    null
                )
            );
        }

        return resultado;
    }

    /**
     * Parsea el JSON devuelto por el proxy (estado de asientos en Redis)
     * a un objeto ProxyEstadoAsientosResponse.
     *
     * Si hay error de parseo o el JSON es nulo, devuelve null y registra un log de error.
     */
    private ProxyEstadoAsientosResponse parsearRedis(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ProxyEstadoAsientosResponse.class);
        } catch (Exception e) {
            log.error("💥 Error parseando JSON Redis", e);
            return null;
        }
    }

    /**
     * Obtiene el estado de UN asiento puntual (fila/columna) para un evento,
     * usando el mapa final de obtenerEstadoActualDeAsientos().
     *
     * @param eventoId ID local del evento
     * @param fila fila del asiento
     * @param columna columna del asiento
     * @return AsientoEstadoDTO o null si no existe ese asiento
     */
    public AsientoEstadoDTO obtenerEstadoAsiento(Long eventoId, int fila, int columna) {
        List<AsientoEstadoDTO> todos = obtenerEstadoActualDeAsientos(eventoId);

        return todos
            .stream()
            .filter(a ->
                a.getFila() != null && a.getColumna() != null &&
                    a.getFila() == fila && a.getColumna() == columna
            )
            .findFirst()
            .orElse(null);
    }

}
