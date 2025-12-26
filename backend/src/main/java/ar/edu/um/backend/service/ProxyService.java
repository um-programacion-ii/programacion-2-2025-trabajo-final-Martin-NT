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

    /** GET /api/proxy/eventos  (Payload 4 completos) */
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

    /** GET /api/proxy/eventos-resumidos (Payload 3 resumidos) */
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

    /** GET /api/proxy/eventos/{id} (Payload 5 detalle) */
    public ProxyEventoDetalleDTO obtenerEventoPorId(Long externalId) {
        if (externalId == null) {
            log.warn("⚠️ [Proxy-Backend] obtenerEventoPorId llamado con externalId=null");
            return null;
        }
        try {
            return proxyWebClient
                .get()
                .uri("/eventos/" + externalId)
                .retrieve()
                .bodyToMono(ProxyEventoDetalleDTO.class)
                .block();
        } catch (WebClientResponseException e) {
            log.error(
                "❌ [Proxy-Backend] Error obteniendo evento detalle externalId={} -> {}",
                externalId,
                e.getResponseBodyAsString(),
                e
            );
            return null;
        } catch (Exception e) {
            log.error("💥 [Proxy-Backend] Error inesperado obteniendo evento detalle externalId={}", externalId, e);
            return null;
        }
    }

    /* ============================================================
       ESTADO DE ASIENTOS (REDIS) / ASIENTOS
       ============================================================ */

    /** GET /api/proxy/eventos/{id}/estado-asientos  (Redis remoto) */
    public ProxyEstadoAsientosResponse listarEstadoAsientosRedis(Long externalId) {
        if (externalId == null) {
            log.warn("⚠️ [Proxy-Backend] estado-asientos llamado con externalId=null");
            return null;
        }
        return getAsientosWrapper("/eventos/" + externalId + "/estado-asientos", externalId, "estado-asientos");
    }

    /** GET /api/proxy/eventos/{id}/asientos (asientos del evento desde cátedra/proxy) */
    public ProxyEstadoAsientosResponse listarAsientosDeEvento(Long externalId) {
        if (externalId == null) {
            log.warn("⚠️ [Proxy-Backend] asientos llamado con externalId=null");
            return null;
        }
        return getAsientosWrapper("/eventos/" + externalId + "/asientos", externalId, "asientos");
    }

    /**
     * Helper común: llama a endpoint que puede devolver:
     * - wrapper: { "eventoId": X, "asientos": [ ... ] }
     * - lista directa: [ {fila, columna, estado, expira, personaActual, ...}, ... ]
     */
    private ProxyEstadoAsientosResponse getAsientosWrapper(String uri, Long externalId, String tag) {
        // Nota: externalId ya viene validado por los métodos públicos, pero lo dejamos por seguridad.
        if (externalId == null) {
            log.warn("⚠️ [Proxy-Backend] {} llamado con externalId=null (uri={})", tag, uri);
            return null;
        }

        try {
            String json = proxyWebClient
                .get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (json == null || json.isBlank()) {
                log.warn("⚠️ [Proxy-Backend] {} devolvió body vacío externalId={}", tag, externalId);
                return null;
            }

            // 1) Intento wrapper
            try {
                ProxyEstadoAsientosResponse wrapper = objectMapper.readValue(json, ProxyEstadoAsientosResponse.class);

                if (wrapper != null) {
                    if (wrapper.getEventoId() == null) {
                        wrapper.setEventoId(externalId);
                    }
                    // Normalizamos asientos null -> lista vacía (evita NPEs aguas abajo)
                    if (wrapper.getAsientos() == null) {
                        wrapper.setAsientos(Collections.emptyList());
                    }
                }

                log.debug("✅ [Proxy-Backend] {} parseado como WRAPPER (externalId={})", tag, externalId);
                return wrapper;

            } catch (Exception ignoreWrapper) {
                // 2) Intento lista directa y la “envuelvo”
                List<AsientoRequestDTO> lista = objectMapper.readValue(
                    json,
                    new TypeReference<List<AsientoRequestDTO>>() {}
                );

                ProxyEstadoAsientosResponse wrapper = new ProxyEstadoAsientosResponse();
                wrapper.setEventoId(externalId);
                wrapper.setAsientos(lista != null ? lista : Collections.emptyList());

                log.debug("✅ [Proxy-Backend] {} parseado como LISTA y envuelto (externalId={})", tag, externalId);
                return wrapper;
            }

        } catch (WebClientResponseException e) {
            log.error("❌ [Proxy-Backend] Error HTTP {} externalId={} -> {}", tag, externalId, e.getResponseBodyAsString(), e);
            return null;
        } catch (Exception e) {
            log.error("💥 [Proxy-Backend] Error inesperado {} externalId={}", tag, externalId, e);
            return null;
        }
    }

    /* ============================================================
       BLOQUEO DE ASIENTOS
       ============================================================ */

    /** POST /api/proxy/eventos/{externalId}/bloqueos (bloqueo real en Redis cátedra) */
    public AsientoBloqueoResponseDTO crearBloqueoEnProxy(AsientoBloqueoRequestDTO dto) {
        if (dto == null || dto.getEventoId() == null) {
            log.warn("⚠️ [Proxy-Backend] crearBloqueoEnProxy llamado con dto/eventoId nulo");
            return null;
        }

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
            log.error("❌ [Proxy-Backend] Error HTTP bloqueando asientos (eventoId={}) -> {}", dto.getEventoId(), e.getResponseBodyAsString(), e);
            return null;
        } catch (Exception e) {
            log.error("💥 [Proxy-Backend] Error inesperado bloqueando asientos (eventoId={})", dto.getEventoId(), e);
            return null;
        }
    }

    /* ============================================================
       VENTAS
       ============================================================ */

    /** POST /api/proxy/eventos/{externalId}/venta */
    public ProxyVentaResponseDTO crearVentaEnProxy(Long externalId, ProxyVentaRequestDTO ventaRequest) {
        if (externalId == null) {
            log.warn("⚠️ [Proxy-Backend] crearVentaEnProxy llamado con externalId=null");
            return null;
        }

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
            log.error("❌ [Proxy-Backend] Error creando venta en proxy (externalId={}) -> {}", externalId, e.getResponseBodyAsString(), e);
            return null;
        } catch (Exception e) {
            log.error("💥 [Proxy-Backend] Error inesperado creando venta en proxy (externalId={})", externalId, e);
            return null;
        }
    }
}
