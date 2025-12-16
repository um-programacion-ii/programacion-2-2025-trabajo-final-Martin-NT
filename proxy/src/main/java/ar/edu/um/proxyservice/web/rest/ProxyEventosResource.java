package ar.edu.um.proxyservice.web.rest;
import ar.edu.um.proxyservice.service.CatServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
/**
 * Controlador REST del proxy para exponer endpoints de eventos
 * hacia el backend (o Postman), delegando en CatServiceClient (Feign).
 */
@RestController //le dice a Spring que esta clase expone endpoints REST
@RequestMapping("/api/proxy") //todos los endpoints de esta clase van a empezar con /api/proxy
public class ProxyEventosResource {

    private static final Logger log = LoggerFactory.getLogger(ProxyEventosResource.class);

    private final CatServiceClient catServiceClient;

    public ProxyEventosResource(CatServiceClient catServiceClient) {
        this.catServiceClient = catServiceClient;
    }

    /**
     * GET /api/proxy/eventos-resumidos
     *
     * Llama a la cátedra vía Feign para obtener la lista de eventos resumidos
     * y devuelve el JSON crudo al cliente.
     */
    @GetMapping("/eventos-resumidos")
    public ResponseEntity<String> listarEventosResumidos() {
        log.info("[ProxyEventosResource] GET /api/proxy/eventos-resumidos");

        String body = catServiceClient.listarEventosResumidos();

        if (body == null) {
            log.warn("[ProxyEventosResource] No se pudo obtener eventos-resumidos desde la cátedra");
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"No se pudo obtener eventos-resumidos desde la cátedra\"}");
        }
        return ResponseEntity.ok(body);
    }

    /**
     * GET /api/proxy/eventos
     *
     * Devuelve eventos completos desde la cátedra (JSON crudo).
     */
    @GetMapping("/eventos")
    public ResponseEntity<String> listarEventosCompletos() {
        log.info("[ProxyEventosResource] GET /api/proxy/eventos");

        String body = catServiceClient.listarEventosCompletos();

        if (body == null) {
            log.warn("[ProxyEventosResource] No se pudo obtener eventos completos desde la cátedra");
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"No se pudo obtener eventos desde la cátedra\"}");
        }
        return ResponseEntity.ok(body);
    }

    /**
     * GET /api/proxy/eventos/{id}
     *
     * Devuelve el detalle de un evento por ID (JSON crudo).
     */
    @GetMapping("/eventos/{id}")
    public ResponseEntity<String> obtenerEventoPorId(@PathVariable Long id) {
        log.info("[ProxyEventosResource] GET /api/proxy/eventos/{}", id);

        String body = catServiceClient.obtenerEventoPorId(id);

        if (body == null) {
            log.warn("[ProxyEventosResource] No se pudo obtener evento {} desde la cátedra", id);
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"No se pudo obtener el evento desde la cátedra\"}");
        }
        return ResponseEntity.ok(body);
    }

    /**
     * GET /api/proxy/eventos/forzar-actualizacion
     *
     * Dispara el endpoint forzar-actualizacion en la cátedra.
     * Por ahora no usamos el body que devuelva, solo confirmamos que se invocó.
     */
    @GetMapping("/eventos/forzar-actualizacion")
    public ResponseEntity<String> forzarActualizacion() {
        log.info("[ProxyEventosResource] POST /api/proxy/eventos/forzar-actualizacion");

        String body = catServiceClient.forzarActualizacion();

        if (body == null) {
            log.warn("[ProxyEventosResource] Error al invocar forzar-actualizacion en la cátedra");
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"No se pudo forzar la actualización en la cátedra\"}");
        }
        // Si quiero devolver JSON fijo:
        // return ResponseEntity.ok("{\"mensaje\":\"forzar-actualizacion invocado correctamente\"}");
        return ResponseEntity.ok(body);
    }




}
