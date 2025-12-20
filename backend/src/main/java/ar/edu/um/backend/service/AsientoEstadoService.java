package ar.edu.um.backend.service;
import ar.edu.um.backend.domain.Asiento;
import ar.edu.um.backend.domain.Evento;
import ar.edu.um.backend.domain.enumeration.AsientoEstado;
import ar.edu.um.backend.repository.AsientoRepository;
import ar.edu.um.backend.repository.EventoRepository;
import ar.edu.um.backend.service.dto.AsientoEstadoDTO;
import ar.edu.um.backend.service.dto.AsientoRequestDTO;
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
 * Servicio encargado de construir el estado ACTUAL de los asientos de un evento,
 * combinando:
 *
 * - Estado persistido en base local (PostgreSQL).
 * - Estado temporal de bloqueos desde Redis (vía proxy-service).
 *
 * Estados internos resultantes:
 * - LIBRE
 * - VENDIDO
 * - BLOQUEADO_VIGENTE
 * - BLOQUEADO_EXPIRADO
 *
 * Estos estados son internos del backend y pueden ser traducidos
 * a otros valores (ej: "Ocupado", "Bloqueado") según el endpoint.
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

    public List<AsientoEstadoDTO> obtenerEstadoActualDeAsientos(Long eventoId) {

        Evento evento = eventoRepository.findById(eventoId)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado")
            );

        List<Asiento> asientosDB =
            asientoRepository.findByEventoIdOrderByFilaAscColumnaAsc(eventoId);

        ProxyEstadoAsientosResponse redisResponse =
            proxyService.listarEstadoAsientosRedis(evento.getExternalId());


        List<AsientoRequestDTO> redisAsientos =
            (redisResponse != null && redisResponse.getAsientos() != null)
                ? redisResponse.getAsientos()
                : Collections.emptyList();

        Map<String, AsientoRequestDTO> redisMap = new HashMap<>();
        for (AsientoRequestDTO a : redisAsientos) {
            redisMap.put(a.getFila() + "-" + a.getColumna(), a);
        }

        List<AsientoEstadoDTO> resultado = new ArrayList<>();
        Instant ahora = Instant.now();

        for (Asiento asientoDB : asientosDB) {

            String key = asientoDB.getFila() + "-" + asientoDB.getColumna();
            AsientoRequestDTO redis = redisMap.get(key);

            // Prioridad absoluta: vendido en DB
            if (asientoDB.getEstado() == AsientoEstado.VENDIDO) {
                resultado.add(
                    new AsientoEstadoDTO(asientoDB.getFila(), asientoDB.getColumna(), "VENDIDO")
                );
                continue;
            }

            // Redis puede indicar bloqueo
            if (redis != null && redis.getExpira() != null) {
                Instant expira = redis.getExpira();

                if (expira.isAfter(ahora)) {
                    resultado.add(
                        new AsientoEstadoDTO(
                            asientoDB.getFila(),
                            asientoDB.getColumna(),
                            "BLOQUEADO_VIGENTE"
                        )
                    );
                } else {
                    resultado.add(
                        new AsientoEstadoDTO(
                            asientoDB.getFila(),
                            asientoDB.getColumna(),
                            "BLOQUEADO_EXPIRADO"
                        )
                    );
                }
                continue;
            }

            // Si no hay nada especial → LIBRE
            resultado.add(
                new AsientoEstadoDTO(
                    asientoDB.getFila(),
                    asientoDB.getColumna(),
                    "LIBRE"
                )
            );
        }

        return resultado;
    }

    public AsientoEstadoDTO obtenerEstadoAsiento(Long eventoId, int fila, int columna) {
        return obtenerEstadoActualDeAsientos(eventoId)
            .stream()
            .filter(a ->
                a.getFila() != null &&
                    a.getColumna() != null &&
                    a.getFila() == fila &&
                    a.getColumna() == columna
            )
            .findFirst()
            .orElse(null);
    }

    private ProxyEstadoAsientosResponse parsearRedis(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ProxyEstadoAsientosResponse.class);
        } catch (Exception e) {
            log.error("💥 Error parseando JSON de Redis", e);
            return null;
        }
    }
}
