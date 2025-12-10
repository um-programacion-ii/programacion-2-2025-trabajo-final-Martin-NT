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
 * Flujo:
 *   Backend → ProxyService (WebClient) → Proxy-Service → Servidor de la cátedra
 *
 * Este servicio encapsula todas las operaciones HTTP contra:
 *   - /api/proxy/eventos-resumidos
 *   - /api/proxy/eventos
 *   - /api/proxy/eventos/{id}
 *   - /api/proxy/forzar-actualizacion
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
     *
     */
    private final WebClient proxyWebClient;

    public ProxyService(WebClient proxyWebClient) {
        this.proxyWebClient = proxyWebClient;
    }

    /**
     * Metodo interno reutilizable para realizar GET al proxy.
     *
     * @param path ruta relativa dentro de /api/proxy
     * @return el body como String, o null si hubo error
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

            log.info("📩 [Proxy-Backend] Respuesta OK {} (bytes={})",
                path, body != null ? body.length() : 0);

            return body;

        } catch (WebClientResponseException e) {
            // Errores HTTP explícitos (400, 404, 500...)
            log.error("❌ [Proxy-Backend] Error HTTP {} en {} → {}",
                e.getRawStatusCode(), path, e.getResponseBodyAsString());
            return null;

        } catch (Exception e) {
            // Cualquier otro error (timeout, conexión, deserialización, etc.)
            log.error("💥 [Proxy-Backend] Error inesperado al llamar {}", path, e);
            return null;
        }
    }

    /**
     * GET /api/proxy/eventos-resumidos
     */
    public String listarEventosResumidos() {
        return doGet("/eventos-resumidos");
    }

    /**
     * GET /api/proxy/eventos
     * Usado por EventoSyncService para sincronizar.
     */
    public String listarEventosCompletos() {
        return doGet("/eventos");
    }

    /**
     * GET /api/proxy/eventos/{id}
     */
    public String obtenerEventoPorId(Long id) {
        return doGet("/eventos/" + id);
    }

    /**
     * GET /api/proxy/forzar-actualizacion
     * Hace que el proxy llame a la cátedra para refrescar el cache.
     */
    public String forzarActualizacion() {
        return doGet("/forzar-actualizacion");
    }
}
