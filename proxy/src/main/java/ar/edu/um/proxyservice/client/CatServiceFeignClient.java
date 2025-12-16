package ar.edu.um.proxyservice.client;
import ar.edu.um.proxyservice.config.CatServiceFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente HTTP declarativo (Feign) para el servidor de la cátedra.
 *
 * Define los endpoints que expone la cátedra y usa catservice.url como base.
 */
@FeignClient(
        name = "cat-service",
        url = "${catservice.url}",
        configuration = CatServiceFeignConfig.class
)
public interface CatServiceFeignClient {

    @GetMapping("/endpoints/v1/eventos-resumidos")
    String listarEventosResumidos();

    @GetMapping("/endpoints/v1/eventos")
    String listarEventosCompletos();

    @GetMapping("/endpoints/v1/evento/{id}")
    String obtenerEventoPorId(@PathVariable("id") Long id);

    @GetMapping("/endpoints/v1/forzar-actualizacion")
    String forzarActualizacion();
}
