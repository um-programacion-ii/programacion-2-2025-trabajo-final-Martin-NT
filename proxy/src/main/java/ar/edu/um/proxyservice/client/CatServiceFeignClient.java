package ar.edu.um.proxyservice.client;
import ar.edu.um.proxyservice.config.CatServiceFeignConfig;
import java.util.Map;

import ar.edu.um.proxyservice.service.dto.BloquearAsientosRequestDTO;
import ar.edu.um.proxyservice.service.dto.BloquearAsientosResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
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
    String realizarVenta(@RequestBody Map<String, Object> ventaJson);

    @PostMapping(
            value = "/api/endpoints/v1/bloquear-asientos",
            consumes = "application/json"
    )
    BloquearAsientosResponseDTO bloquearAsientos(
            @RequestBody BloquearAsientosRequestDTO body
    );

    @GetMapping("/api/endpoints/v1/listar-ventas")
    String listarVentas();

    @GetMapping("/api/endpoints/v1/listar-venta/{id}")
    String listarVentaPorId(@PathVariable("id") Long id);


}
