package ar.edu.um.proxyservice.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente HTTP del proxy para comunicarse con el servidor de la cátedra (CatService).
 *
 * Este servicio centraliza todas las llamadas HTTP hacia la cátedra:
 *  - eventos resumidos
 *  - eventos completos
 *  - detalle de evento por ID
 *  - forzar actualización
 *
 * Por ahora devuelve JSON crudo (String) y loguea las respuestas.
 */
@Service
public class CatServiceClient {

    private static final Logger log = LoggerFactory.getLogger(CatServiceClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CatServiceClient(
            RestTemplate restTemplate,
            @Value("${catservice.url}") String baseUrl // Inyecta la URL base del server de la cátedra.
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * GET /endpoints/v1/eventos-resumidos
     */
    public String listarEventosResumidos() {
        String url = baseUrl + "/endpoints/v1/eventos-resumidos";
        return doGet(url, "listarEventosResumidos");
    }

    /**
     * GET /endpoints/v1/eventos
     */
    public String listarEventosCompletos() {
        String url = baseUrl + "/endpoints/v1/eventos";
        return doGet(url, "listarEventosCompletos");
    }

    /**
     * GET /endpoints/v1/evento/{id}
     */
    public String obtenerEventoPorId(Long id) {
        String url = baseUrl + "/endpoints/v1/evento/" + id;
        return doGet(url, "obtenerEventoPorId");
    }

    /**
     * GET /endpoints/v1/forzar-actualizacion
     */
    public String forzarActualizacion() {
        String url = baseUrl + "/endpoints/v1/forzar-actualizacion";
        return doGet(url, "forzarActualizacion");
    }

    /**
     * Metodo interno que hace el GET y maneja logs y errores comunes.
     */
    private String doGet(String url, String operacion) {
        try {
            log.info("[CatServiceClient] Llamando a {} -> {}", operacion, url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            log.info("[CatServiceClient] Respuesta {}: status={}, bodyLength={}",
                    operacion,
                    response.getStatusCode(),
                    response.getBody() != null ? response.getBody().length() : 0
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("[CatServiceClient] Error llamando a {} -> {}", operacion, url, e);
            return null; // por ahora devolvemos null en caso de error
        }
    }
}
