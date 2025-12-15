package ar.edu.um.proxyservice.service;
import ar.edu.um.proxyservice.client.CatServiceFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Map;
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

    public String listarEventosResumidos() {
        String operacion = "listarEventosResumidos";
        try {
            log.info("🎓 [Cátedra] Llamando a {} vía Feign", operacion);
            String body = feignClient.listarEventosResumidos();
            log.info("🎓 [Cátedra] Respuesta {} -> bodyLength={}", operacion, body != null ? body.length() : null);
            return body;
        } catch (Exception e) {
            log.error("🎓 [Cátedra] Error llamando a {} vía Feign", operacion, e);
            return null;
        }
    }

    public String listarEventosCompletos() {
        String operacion = "listarEventosCompletos";
        try {
            log.info("🎓 [Cátedra] Llamando a {} vía Feign", operacion);
            String body = feignClient.listarEventosCompletos();
            log.info("🎓 [Cátedra] Respuesta {} -> bodyLength={}", operacion, body != null ? body.length() : null);
            return body;
        } catch (Exception e) {
            log.error("🎓 [Cátedra] Error llamando a {} vía Feign", operacion, e);
            return null;
        }
    }

    public String obtenerEventoPorId(Long id) {
        String operacion = "obtenerEventoPorId";
        try {
            log.info("🎓 [Cátedra] Llamando a {} ({}) vía Feign", operacion, id);
            String body = feignClient.obtenerEventoPorId(id);
            log.info("🎓 [Cátedra] Respuesta {}({}) -> bodyLength={}", operacion, id, body != null ? body.length() : null);
            return body;
        } catch (Exception e) {
            log.error("🎓 [Cátedra] Error llamando a {} ({}) vía Feign", operacion, id, e);
            return null;
        }
    }

    public String forzarActualizacion() {
        String operacion = "forzarActualizacion";
        try {
            log.info("🎓 [Cátedra] Llamando a {} vía Feign", operacion);
            String body = feignClient.forzarActualizacion();
            log.info("🎓 [Cátedra] Respuesta {} -> bodyLength={}", operacion, body != null ? body.length() : null);
            return body;
        } catch (Exception e) {
            log.error("🎓 [Cátedra] Error llamando a {} vía Feign", operacion, e);
            return null;
        }
    }

    /**
     * POST /api/endpoints/v1/realizar-venta  (P7 - Venta de asientos por evento)
     */
    public void crearVenta(Map<String, Object> ventaJson) {
        String operacion = "crearVenta";
        try {
            log.info("🎓 [Cátedra] Llamando a {} vía Feign", operacion);
            feignClient.crearVenta(ventaJson); // 200 OK sin body → está bien
            log.info("🎓 [Cátedra] Venta confirmada (200 OK) desde la cátedra");
        } catch (Exception e) {
            log.error("🎓 [Cátedra] Error llamando a {} vía Feign", operacion, e);
            throw e; // dejamos que el proxy decida qué hacer
        }
    }

}
