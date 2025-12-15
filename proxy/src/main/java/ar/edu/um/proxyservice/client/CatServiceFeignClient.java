package ar.edu.um.proxyservice.client;
import ar.edu.um.proxyservice.config.CatServiceFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;
/**
 * Cliente HTTP declarativo (Feign) para el servidor de la cátedra.
 * Define los endpoints que expone la cátedra y usa catservice.url como base.
 */
@FeignClient(
        name = "cat-service",
        url = "${catservice.url}",
        configuration = CatServiceFeignConfig.class
)
public interface CatServiceFeignClient {

    @GetMapping("/api/endpoints/v1/eventos-resumidos")
    String listarEventosResumidos();

    @GetMapping("/api/endpoints/v1/eventos")
    String listarEventosCompletos();

    @GetMapping("/api/endpoints/v1/evento/{id}")
    String obtenerEventoPorId(@PathVariable("id") Long id);

    @GetMapping("/api/endpoints/v1/forzar-actualizacion")
    String forzarActualizacion();

    @PostMapping(
            value = "/api/endpoints/v1/realizar-venta",
            consumes = "application/json"
    )
    void crearVenta(@RequestBody Map<String, Object> ventaJson);

    @PostMapping(
            value = "/api/endpoints/v1/bloquear-asientos",
            consumes = "application/json"
    )
    void bloquearAsiento(@RequestBody Map<String, Object> bloqueoJson);

}
