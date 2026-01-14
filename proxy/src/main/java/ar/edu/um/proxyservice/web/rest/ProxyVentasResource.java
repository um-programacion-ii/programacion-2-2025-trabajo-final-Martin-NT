package ar.edu.um.proxyservice.web.rest;
import ar.edu.um.proxyservice.service.CatServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proxy")
public class ProxyVentasResource {

    private static final Logger log = LoggerFactory.getLogger(ProxyVentasResource.class);

    private final CatServiceClient catServiceClient;

    public ProxyVentasResource(CatServiceClient catServiceClient) {
        this.catServiceClient = catServiceClient;
    }

    @GetMapping(value = "/listar-ventas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> listarVentas() {
        log.info("💸 [Proxy] GET /api/proxy/listar-ventas");
        String body = catServiceClient.listarVentas();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @GetMapping(value = "/listar-venta/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> listarVentaPorId(@PathVariable Long id) {
        log.info("💸 [Proxy] GET /api/proxy/listar-venta/{}", id);
        String body = catServiceClient.listarVentaPorId(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
