package ar.edu.um.proxyservice.web.rest;
import ar.edu.um.proxyservice.service.CatServiceClient;
import ar.edu.um.proxyservice.service.EstadoAsientosRedisService;
import ar.edu.um.proxyservice.service.dto.BloquearAsientosRequestDTO;
import ar.edu.um.proxyservice.service.dto.BloquearAsientosResponseDTO;
import ar.edu.um.proxyservice.service.dto.EstadoAsientosRemotoDTO;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proxy")
public class ProxyEventosResource {

    private static final Logger log = LoggerFactory.getLogger(ProxyEventosResource.class);

    private final CatServiceClient catServiceClient;
    private final EstadoAsientosRedisService estadoAsientosRedisService;

    public ProxyEventosResource(CatServiceClient catServiceClient, EstadoAsientosRedisService estadoAsientosRedisService) {
        this.catServiceClient = catServiceClient;
        this.estadoAsientosRedisService = estadoAsientosRedisService;
    }

    @GetMapping(value = "/eventos-resumidos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> listarEventosResumidos() {
        log.info("🌐 [Proxy] GET /api/proxy/eventos-resumidos");
        String body = catServiceClient.listarEventosResumidos();
        if (body == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"No se pudo obtener eventos-resumidos desde la cátedra\"}");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @GetMapping(value = "/eventos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> listarEventosCompletos() {
        log.info("🌐 [Proxy] GET /api/proxy/eventos");
        String body = catServiceClient.listarEventosCompletos();
        if (body == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"No se pudo obtener eventos desde la cátedra\"}");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @GetMapping(value = "/eventos/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> obtenerEventoPorId(@PathVariable Long id) {
        log.info("🌐 [Proxy] GET /api/proxy/eventos/{}", id);
        String body = catServiceClient.obtenerEventoPorId(id);
        if (body == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"No se pudo obtener el evento desde la cátedra\"}");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @GetMapping(value = "/eventos/forzar-actualizacion", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> forzarActualizacion() {
        log.info("🌐 [Proxy] GET /api/proxy/eventos/forzar-actualizacion");
        String body = catServiceClient.forzarActualizacion();
        if (body == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"No se pudo forzar la actualización en la cátedra\"}");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @GetMapping(value = "/eventos/{id}/estado-asientos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> obtenerEstadoAsientos(@PathVariable Long id) {
        log.info("🌐 [Proxy] GET /api/proxy/eventos/{}/estado-asientos", id);
        try {
            EstadoAsientosRemotoDTO dto = estadoAsientosRedisService.obtenerEstadoAsientos(id);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("🌐 [Proxy] Error consultando Redis para evento {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Error consultando Redis para el estado de asientos\"}");
        }
    }

    @GetMapping(value = "/eventos/{id}/asientos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> obtenerAsientosEvento(@PathVariable Long id) {
        // En tu proyecto /asientos es alias del estado en Redis remoto
        log.info("🌐 [Proxy] GET /api/proxy/eventos/{}/asientos", id);
        try {
            EstadoAsientosRemotoDTO dto = estadoAsientosRedisService.obtenerEstadoAsientos(id);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("🌐 [Proxy] Error consultando Redis (asientos) para evento {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Error consultando Redis para asientos\"}");
        }
    }

    @PostMapping(value = "/eventos/{id}/bloqueos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BloquearAsientosResponseDTO> crearBloqueoEvento(
            @PathVariable Long id,
            @RequestBody BloquearAsientosRequestDTO request
    ) {
        log.info("🌐 [Proxy] POST /api/proxy/eventos/{}/bloqueos", id);

        // Validación básica
        if (request == null || request.getAsientos() == null || request.getAsientos().isEmpty()) {
            return ResponseEntity.badRequest().build(); // el backend ya sabe interpretar 400
        }

        // Normalizamos eventoId (externalId de la cátedra)
        if (request.getEventoId() == null) {
            request.setEventoId(id);
        }

        // Aviso si no coincide (no bloquea la operación)
        if (!id.equals(request.getEventoId())) {
            log.warn("⚠️ [Proxy] Bloqueo: path id={} != body eventoId={} (se enviará body).", id, request.getEventoId());
        }

        try {
            BloquearAsientosResponseDTO resp = catServiceClient.bloquearAsientos(request);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("💥 [Proxy] Error al bloquear asientos en la cátedra eventoId={}", request.getEventoId(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @PostMapping(value = "/eventos/{id}/venta", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> crearVentaEvento(
            @PathVariable Long id,
            @RequestBody Map<String, Object> ventaJson
    ) {
        log.info("🌐 [Proxy] POST /api/proxy/eventos/{}/venta", id);
        log.debug("🌐 [Proxy] Payload venta recibido: {}", ventaJson);

        try {
            // reenviamos tal cual
            String respuesta = catServiceClient.realizarVenta(ventaJson);

            // si hay body → lo devolvemos como JSON
            if (respuesta != null && !respuesta.isBlank()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(respuesta);
            }

            // si no hay body → 200 OK sin body (válido según consigna)
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("🌐 [Proxy] Error al crear venta en la cátedra para evento {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"No se pudo crear la venta en la cátedra\"}");
        }
    }
}
