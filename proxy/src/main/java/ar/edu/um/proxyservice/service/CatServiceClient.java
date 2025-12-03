package ar.edu.um.proxyservice.service;
import ar.edu.um.proxyservice.client.CatServiceFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Fachada sobre el cliente Feign CatServiceFeignClient.
 *
 * Delega las llamadas HTTP hacia la cátedra y centraliza logs y manejo básico de errores.
 * La autenticación (JWT) se aplica a nivel de configuración Feign (CatServiceFeignConfig).
 */
@Service
public class CatServiceClient {
    private static final Logger log = LoggerFactory.getLogger(CatServiceClient.class);

    private final CatServiceFeignClient feignClient;

    public CatServiceClient(CatServiceFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    /**
     * GET /endpoints/v1/eventos-resumidos
     */
    public String listarEventosResumidos() {
        String operacion = "listarEventosResumidos";
        try {
            log.info("[CatServiceClient] Llamando a {} vía Feign", operacion);
            String body = feignClient.listarEventosResumidos();
            log.info("[CatServiceClient] Respuesta {} -> bodyLength={}",
                    operacion,
                    body != null ? body.length() : null
            );
            return body;
        } catch (Exception e) {
            log.error("[CatServiceClient] Error llamando a {} vía Feign", operacion, e);
            return null;
        }
    }

    /**
     * GET /endpoints/v1/eventos
     */
    public String listarEventosCompletos() {
        String operacion = "listarEventosCompletos";
        try {
            log.info("[CatServiceClient] Llamando a {} vía Feign", operacion);
            String body = feignClient.listarEventosCompletos();
            log.info("[CatServiceClient] Respuesta {} -> bodyLength={}",
                    operacion,
                    body != null ? body.length() : null
            );
            return body;
        } catch (Exception e) {
            log.error("[CatServiceClient] Error llamando a {} vía Feign", operacion, e);
            return null;
        }
    }

    /**
     * GET /endpoints/v1/evento/{id}
     */
    public String obtenerEventoPorId(Long id) {
        String operacion = "obtenerEventoPorId";
        try {
            log.info("[CatServiceClient] Llamando a {} ({}) vía Feign", operacion, id);
            String body = feignClient.obtenerEventoPorId(id);
            log.info("[CatServiceClient] Respuesta {}({}) -> bodyLength={}",
                    operacion,
                    id,
                    body != null ? body.length() : null
            );
            return body;
        } catch (Exception e) {
            log.error("[CatServiceClient] Error llamando a {} ({}) vía Feign", operacion, id, e);
            return null;
        }
    }

    /**
     * GET /endpoints/v1/forzar-actualizacion
     */
    public String forzarActualizacion() {
        String operacion = "forzarActualizacion";
        try {
            log.info("[CatServiceClient] Llamando a {} vía Feign", operacion);
            String body = feignClient.forzarActualizacion();
            log.info("[CatServiceClient] Respuesta {} -> bodyLength={}",
                    operacion,
                    body != null ? body.length() : null
            );
            return body;
        } catch (Exception e) {
            log.error("[CatServiceClient] Error llamando a {} vía Feign", operacion, e);
            return null;
        }
    }
}
