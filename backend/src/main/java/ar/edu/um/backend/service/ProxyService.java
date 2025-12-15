package ar.edu.um.backend.service;
import ar.edu.um.backend.service.dto.ProxyAsientoDTO;
import ar.edu.um.backend.service.dto.ProxyVentaDTO;
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
     * Metodo interno reutilizable para realizar GET al proxy.
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
     * Metodo interno reutilizable para realizar POST al proxy.
     *
     * Loguea el endpoint y el tipo de body.
     * Hace el POST con el proxyWebClient (ya lleva el Bearer).
     * Loguea la respuesta o el error (HTTP o de conexión).
     *
     * Maneja:
     *  - logs de request/response,
     *  - errores HTTP (4xx/5xx),
     *  - otros errores de conexión/timeout.
     *
     * @param path ruta relativa dentro de /api/proxy (ej: "/eventos/1/venta").
     * @param body cuerpo a enviar como JSON (DTO).
     * @return el body como String, o null si hubo error.
     */
    private String doPost(String path, Object body) {
        try {
            log.info(
                "🌐 [Proxy-Backend] POST {} (bodyClass={})",
                path,
                body != null ? body.getClass().getSimpleName() : "null"
            );

            String response = proxyWebClient
                .post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .defaultIfEmpty("")   // si el body está vacío, devuelve "" en vez de null
                .block();

            log.info(
                "📩 [Proxy-Backend] Respuesta OK POST {} (bytes={})",
                path,
                response != null ? response.length() : 0
            );

            return response;
        } catch (WebClientResponseException e) {
            log.error(
                "❌ [Proxy-Backend] Error HTTP {} en POST {} → {}",
                e.getRawStatusCode(),
                path,
                e.getResponseBodyAsString()
            );
            return null;
        } catch (Exception e) {
            log.error("💥 [Proxy-Backend] Error inesperado al hacer POST {}", path, e);
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
     * Devuelve el JSON crudo de asientos del evento (wrapper con eventoId + lista de asientos).
     *
     * @param externalId ID del evento en la cátedra.
     * @return JSON crudo con el estado de asientos, o null si hubo error.
     */
    public String listarAsientosDeEvento(Long externalId) {
        return doGet("/eventos/" + externalId + "/asientos");
    }

    /**
     * GET /api/proxy/eventos/{id}/estado-asientos
     * Obtiene el estado de asientos de un evento desde Redis (vía proxy).
     */
    public String listarEstadoAsientosRedis(Long externalId) {
        return doGet("/eventos/" + externalId + "/estado-asientos");
    }

    /**
     * POST /api/proxy/eventos/{externalId}/venta
     *
     * Envía una venta real al proxy, que a su vez la manda a la cátedra.
     * Usa el DTO ProxyVentaDTO como cuerpo.
     *
     * @param externalId ID del evento en la cátedra.
     * @param venta DTO con cliente, asientos y total.
     * @return JSON crudo devuelto por el proxy/cátedra, o null si hubo error.
     */
    public String crearVentaEnProxy(Long externalId, ProxyVentaDTO venta) {
        log.info(
            "💸 [Proxy-Backend] Enviando venta al proxy para externalId={}, asientos={}",
            externalId,
            venta != null && venta.getAsientos() != null ? venta.getAsientos().size() : 0
        );

        String path = "/eventos/" + externalId + "/venta";
        String respuesta = doPost(path, venta);

        log.info(
            "💸 [Proxy-Backend] Respuesta de venta desde proxy para externalId={} -> {}",
            externalId,
            respuesta
        );

        return respuesta;
    }

    /**
     * POST /api/proxy/eventos/{externalId}/bloqueos
     *
     * Envía un bloqueo de asiento al proxy, que a su vez lo manda a la cátedra.
     * Usa ProxyAsientoDTO como body (solo fila y columna).
     *
     * @param externalId ID del evento en la cátedra.
     * @param fila fila del asiento a bloquear (>=1)
     * @param columna columna del asiento a bloquear (>=1)
     * @return JSON crudo devuelto por el proxy/cátedra, o null si hubo error.
     */
    public String crearBloqueoEnProxy(Long externalId, Integer fila, Integer columna) {
        ProxyAsientoDTO body = new ProxyAsientoDTO();
        body.setFila(fila);
        body.setColumna(columna);

        log.info(
            "🔒 [Proxy-Backend] Enviando bloqueo al proxy: externalId={}, fila={}, columna={}",
            externalId,
            fila,
            columna
        );

        String path = "/eventos/" + externalId + "/bloqueos";
        String respuesta = doPost(path, body);

        log.info(
            "🔒 [Proxy-Backend] Respuesta bloqueo desde proxy para externalId={} -> {}",
            externalId,
            respuesta
        );

        return respuesta;
    }

}
