package ar.edu.um.backend.service;

import ar.edu.um.backend.service.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Servicio de integración encargado de comunicarse con el proxy-service local.
 *
 * Reglas:
 * - NO contiene lógica de negocio.
 * - NO decide estados.
 * - NO maneja TTL.
 *
 * Su responsabilidad es:
 *   Backend → Proxy-Service → Cátedra
 *
 * Cada metodo representa un endpoint concreto del proxy y devuelve DTOs tipados.
 */
@Service
public class ProxyService {

    private static final Logger log = LoggerFactory.getLogger(ProxyService.class);

    private final WebClient proxyWebClient;
    private final ObjectMapper objectMapper;

    public ProxyService(WebClient proxyWebClient, ObjectMapper objectMapper) {
        this.proxyWebClient = proxyWebClient;
        this.objectMapper = objectMapper;
    }

    /* ============================================================
       EVENTOS
       ============================================================ */

    /**
     * GET /api/proxy/eventos
     * Debe mapear a Payload 4 (eventos completos).
     */
    public List<ProxyEventoDetalleDTO> listarEventosCompletos() {
        try {
            return proxyWebClient
                .get()
                .uri("/eventos")
                .retrieve()
                .bodyToFlux(ProxyEventoDetalleDTO.class)
                .collectList()
                .block();
        } catch (WebClientResponseException e) {
            log.error("❌ [Proxy-Backend] Error listando eventos completos: {}", e.getResponseBodyAsString(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("💥 [Proxy-Backend] Error inesperado listando eventos completos", e);
            return Collections.emptyList();
        }
    }

    /**
     * GET /api/proxy/eventos-resumidos
     * Debe mapear a Payload 3 (eventos resumidos).
     */
    public List<ProxyEventoResumenDTO> listarEventosResumidos() {
        try {
            return proxyWebClient
                .get()
                .uri("/eventos-resumidos")
                .retrieve()
                .bodyToFlux(ProxyEventoResumenDTO.class)
                .collectList()
                .block();
        } catch (WebClientResponseException e) {
            log.error("❌ [Proxy-Backend] Error listando eventos resumidos: {}", e.getResponseBodyAsString(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("💥 [Proxy-Backend] Error inesperado listando eventos resumidos", e);
            return Collections.emptyList();
        }
    }

    /**
     * GET /api/proxy/eventos/{id}
     * Debe mapear a Payload 5 (evento completo por id).
     *
     * Importante: NO devuelve EventoDTO (local). Devuelve DTO remoto.
     */
    public ProxyEventoDetalleDTO obtenerEventoPorId(Long externalId) {
        try {
            return proxyWebClient
                .get()
                .uri("/eventos/" + externalId)
                .retrieve()
                .bodyToMono(ProxyEventoDetalleDTO.class)
                .block();
        } catch (WebClientResponseException e) {
            log.error("❌ [Proxy-Backend] Error obteniendo evento detalle externalId={} -> {}", externalId, e.getResponseBodyAsString(), e);
            return null;
        } catch (Exception e) {
            log.error("💥 [Proxy-Backend] Error inesperado obteniendo evento detalle externalId={}", externalId, e);
            return null;
        }
    }

    /* ============================================================
       ESTADO DE ASIENTOS (REDIS)
       ============================================================ */

    /**
     * GET /api/proxy/eventos/{id}/estado-asientos
     *
     * Idealmente el proxy debería devolver un wrapper:
     *   { "eventoId": X, "asientos": [ ... ] }
     *
     * Como aún no confirmaste qué devuelve tu proxy, este método soporta:
     * - wrapper (ProxyEstadoAsientosResponse)
     * - lista directa ([ {fila, columna, estado, expira, ...}, ... ])
     */
    public ProxyEstadoAsientosResponse listarEstadoAsientosRedis(Long externalId) {
        try {
            String json = proxyWebClient
                .get()
                .uri("/eventos/" + externalId + "/estado-asientos")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (json == null || json.isBlank()) {
                return null;
            }

            // 1) Intento wrapper
            try {
                ProxyEstadoAsientosResponse wrapper = objectMapper.readValue(json, ProxyEstadoAsientosResponse.class);
                // si vino sin eventoId, igual lo seteamos para consistencia
                if (wrapper != null && wrapper.getEventoId() == null) {
                    wrapper.setEventoId(externalId);
                }
                return wrapper;
            } catch (Exception ignore) {
                // 2) Intento lista directa y la “envuelvo”
                List<AsientoRequestDTO> lista = objectMapper.readValue(json, new TypeReference<List<AsientoRequestDTO>>() {});
                ProxyEstadoAsientosResponse wrapper = new ProxyEstadoAsientosResponse();
                wrapper.setEventoId(externalId);
                wrapper.setAsientos(lista);
                return wrapper;
            }

        } catch (WebClientResponseException e) {
            log.error("❌ [Proxy-Backend] Error obteniendo estado-asientos externalId={} -> {}", externalId, e.getResponseBodyAsString(), e);
            return null;
        } catch (Exception e) {
            log.error("💥 [Proxy-Backend] Error inesperado obteniendo estado-asientos externalId={}", externalId, e);
            return null;
        }
    }

    /* ============================================================
       ASIENTOS DE EVENTO (CÁTEDRA / EVENTOS/{id}/ASIENTOS)
       ============================================================ */

    /**
     * GET /api/proxy/eventos/{id}/asientos
     *
     * Devuelve los asientos del evento desde la cátedra (vía proxy).
     *
     * Soporta 2 formatos (por si el proxy cambia):
     *  - wrapper: { "eventoId": X, "asientos": [ ... ] }
     *  - lista directa: [ {fila, columna, estado, expira, personaActual, ...}, ... ]
     */
    public ProxyEstadoAsientosResponse listarAsientosDeEvento(Long externalId) {
        try {
            String json = proxyWebClient
                .get()
                .uri("/eventos/" + externalId + "/asientos")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (json == null || json.isBlank()) {
                return null;
            }

            // 1) Intento wrapper
            try {
                ProxyEstadoAsientosResponse wrapper = objectMapper.readValue(json, ProxyEstadoAsientosResponse.class);
                if (wrapper != null && wrapper.getEventoId() == null) {
                    wrapper.setEventoId(externalId);
                }
                return wrapper;
            } catch (Exception ignore) {
                // 2) Intento lista directa y la “envuelvo”
                List<AsientoRequestDTO> lista = objectMapper.readValue(
                    json,
                    new TypeReference<List<AsientoRequestDTO>>() {}
                );

                ProxyEstadoAsientosResponse wrapper = new ProxyEstadoAsientosResponse();
                wrapper.setEventoId(externalId);
                wrapper.setAsientos(lista);
                return wrapper;
            }

        } catch (WebClientResponseException e) {
            log.error(
                "❌ [Proxy-Backend] Error obteniendo asientos externalId={} -> {}",
                externalId,
                e.getResponseBodyAsString(),
                e
            );
            return null;
        } catch (Exception e) {
            log.error("💥 [Proxy-Backend] Error inesperado obteniendo asientos externalId={}", externalId, e);
            return null;
        }
    }


    /* ============================================================
       BLOQUEO DE ASIENTOS
       ============================================================ */

    /**
     * POST /api/proxy/eventos/{externalId}/bloqueos
     *
     * Ejecuta el bloqueo real de asientos en la cátedra (Redis remoto).
     * La cátedra decide si el bloqueo es exitoso o no.
     */
    public AsientoBloqueoResponseDTO crearBloqueoEnProxy(AsientoBloqueoRequestDTO dto) {
        try {
            log.info(
                "🔒 [Proxy-Backend] Enviando bloqueo al proxy: eventoId={}, asientos={}",
                dto.getEventoId(),
                dto.getAsientos() != null ? dto.getAsientos().size() : 0
            );

            return proxyWebClient
                .post()
                .uri("/eventos/" + dto.getEventoId() + "/bloqueos")
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(AsientoBloqueoResponseDTO.class)
                .block();

        } catch (WebClientResponseException e) {
            log.error(
                "❌ [Proxy-Backend] Error HTTP bloqueando asientos (eventoId={}) -> {}",
                dto.getEventoId(),
                e.getResponseBodyAsString(),
                e
            );
            return null;
        } catch (Exception e) {
            log.error("💥 [Proxy-Backend] Error inesperado bloqueando asientos (eventoId={})", dto.getEventoId(), e);
            return null;
        }
    }

    /* ============================================================
       VENTAS
       ============================================================ */

    /**
     * POST /api/proxy/eventos/{externalId}/venta
     *
     * Request (Payload 7 entrada): ProxyVentaRequestDTO
     * Response (Payload 7 salida): ProxyVentaResponseDTO
     */
    public ProxyVentaResponseDTO crearVentaEnProxy(Long externalId, ProxyVentaRequestDTO ventaRequest) {
        try {
            log.info(
                "💸 [Proxy-Backend] Enviando venta al proxy: externalId={}, asientos={}",
                externalId,
                ventaRequest != null && ventaRequest.getAsientos() != null ? ventaRequest.getAsientos().size() : 0
            );

            return proxyWebClient
                .post()
                .uri("/eventos/" + externalId + "/venta")
                .bodyValue(ventaRequest)
                .retrieve()
                .bodyToMono(ProxyVentaResponseDTO.class)
                .block();

        } catch (WebClientResponseException e) {
            log.error(
                "❌ [Proxy-Backend] Error creando venta en proxy (externalId={}) -> {}",
                externalId,
                e.getResponseBodyAsString(),
                e
            );
            return null;
        } catch (Exception e) {
            log.error("💥 [Proxy-Backend] Error inesperado creando venta en proxy (externalId={})", externalId, e);
            return null;
        }
    }
}
