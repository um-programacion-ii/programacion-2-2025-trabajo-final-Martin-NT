package ar.edu.um.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Servicio encargado de realizar llamadas HTTP desde el backend del alumno
 * hacia el proxy-service local, el cual a su vez se comunica con la cátedra.
 *
 * Flujo general:
 *   Backend → ProxyService (WebClient) → Proxy-Service → Servidor de la cátedra
 *
 * Este servicio encapsula todas las operaciones HTTP contra:
 *   - /api/proxy/eventos-resumidos
 *   - /api/proxy/eventos
 *   - /api/proxy/eventos/{id}
 *   - /api/proxy/forzar-actualizacion
 *   - /api/proxy/eventos/{id}/asientos
 *
 * Todas las llamadas usan el WebClient ya preconfigurado en ProxyWebClientConfig,
 * incluyendo el token JWT configurado en PROXY_TOKEN.
 */
@Service
public class ProxyService {

    private static final Logger log = LoggerFactory.getLogger(ProxyService.class);

    /**
     * WebClient inyectado desde ProxyWebClientConfig.
     *
     * Este WebClient ya lleva:
     *  ✔ baseUrl = PROXY_BASE_URL
     *  ✔ Authorization: Bearer PROXY_TOKEN
     */
    private final WebClient proxyWebClient;

    public ProxyService(WebClient proxyWebClient) {
        this.proxyWebClient = proxyWebClient;
    }

    /**
     * Método interno reutilizable para realizar GET al proxy.
     *
     * Maneja:
     *  - logs de request/response,
     *  - errores HTTP (4xx/5xx),
     *  - otros errores de conexión/timeout.
     *
     * @param path ruta relativa dentro de /api/proxy (ej: "/eventos").
     * @return el body como String, o null si hubo error.
     */
    private String doGet(String path) {
        try {
            log.info("🌐 [Proxy-Backend] GET {}", path);

            // Llamada HTTP al proxy-service
            String body = proxyWebClient
                .get()
                .uri(path)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // Bloqueante: OK porque se usa en servicio interno

            log.info(
                "📩 [Proxy-Backend] Respuesta OK {} (bytes={})",
                path,
                body != null ? body.length() : 0
            );

            return body;

        } catch (WebClientResponseException e) {
            // Errores HTTP explícitos (400, 404, 500...)
            log.error(
                "❌ [Proxy-Backend] Error HTTP {} en {} → {}",
                e.getRawStatusCode(),
                path,
                e.getResponseBodyAsString()
            );
            return null;

        } catch (Exception e) {
            // Cualquier otro error (timeout, conexión, deserialización, etc.)
            log.error("💥 [Proxy-Backend] Error inesperado al llamar {}", path, e);
            return null;
        }
    }

    /**
     * Llama a GET /api/proxy/eventos-resumidos en el proxy-service.
     *
     * @return JSON crudo con la lista de eventos resumidos, o null si hubo error.
     */
    public String listarEventosResumidos() {
        return doGet("/eventos-resumidos");
    }

    /**
     * Llama a GET /api/proxy/eventos en el proxy-service.
     * Usado por {@link EventoSyncService} para sincronizar eventos completos.
     *
     * @return JSON crudo con la lista de eventos completos, o null si hubo error.
     */
    public String listarEventosCompletos() {
        return doGet("/eventos");
    }

    /**
     * Llama a GET /api/proxy/eventos/{id} en el proxy-service.
     *
     * @param id ID del evento en la cátedra.
     * @return JSON crudo con el detalle del evento, o null si hubo error.
     */
    public String obtenerEventoPorId(Long id) {
        return doGet("/eventos/" + id);
    }

    /**
     * Llama a GET /api/proxy/forzar-actualizacion en el proxy-service.
     *
     * Hace que el proxy llame a la cátedra para refrescar el cache de eventos/asientos.
     *
     * @return JSON crudo de respuesta, o null si hubo error.
     */
    public String forzarActualizacion() {
        return doGet("/eventos/forzar-actualizacion");
    }

    /**
     * Llama a GET /api/proxy/eventos/{id}/asientos en el proxy-service.
     *
     * Devuelve el JSON crudo de asientos del evento (wrapper con eventoId + lista de asientos).
     *
     * @param externalId ID del evento en la cátedra.
     * @return JSON crudo con el estado de asientos, o null si hubo error.
     */
    public String listarAsientosDeEvento(Long externalId) {
        return doGet("/eventos/" + externalId + "/asientos");
    }
}
