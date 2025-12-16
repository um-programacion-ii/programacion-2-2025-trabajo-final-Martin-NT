package ar.edu.um.proxyservice.web.rest;
import ar.edu.um.proxyservice.service.CatServiceClient;
import ar.edu.um.proxyservice.service.EstadoAsientosRedisService;
import ar.edu.um.proxyservice.service.dto.AsientoRemotoDTO;
import ar.edu.um.proxyservice.service.dto.EstadoAsientosRemotoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
/**
 * Controlador REST del proxy para exponer endpoints de eventos
 * hacia el backend (o Postman), delegando en CatServiceClient (Feign).
 */
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

    @GetMapping("/eventos-resumidos")
    public ResponseEntity<String> listarEventosResumidos() {
        log.info("🌐 [Proxy] GET /api/proxy/eventos-resumidos");
        String body = catServiceClient.listarEventosResumidos();
        if (body == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"No se pudo obtener eventos-resumidos desde la cátedra\"}");
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/eventos")
    public ResponseEntity<String> listarEventosCompletos() {
        log.info("🌐 [Proxy] GET /api/proxy/eventos");
        String body = catServiceClient.listarEventosCompletos();
        if (body == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"No se pudo obtener eventos desde la cátedra\"}");
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/eventos/{id}")
    public ResponseEntity<String> obtenerEventoPorId(@PathVariable Long id) {
        log.info("🌐 [Proxy] GET /api/proxy/eventos/{}", id);
        String body = catServiceClient.obtenerEventoPorId(id);
        if (body == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"No se pudo obtener el evento desde la cátedra\"}");
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/eventos/forzar-actualizacion")
    public ResponseEntity<String> forzarActualizacion() {
        log.info("🌐 [Proxy] GET /api/proxy/eventos/forzar-actualizacion");
        String body = catServiceClient.forzarActualizacion();
        if (body == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"No se pudo forzar la actualización en la cátedra\"}");
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/eventos/{id}/estado-asientos")
    public ResponseEntity<?> obtenerEstadoAsientos(@PathVariable Long id) {
        log.info("🌐 [Proxy] GET /api/proxy/eventos/{}/estado-asientos", id);
        try {
            EstadoAsientosRemotoDTO dto = estadoAsientosRedisService.obtenerEstadoAsientos(id);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("🌐 [Proxy] Error consultando Redis para evento {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"Error consultando Redis para el estado de asientos\"}");
        }
    }

    @GetMapping("/eventos/{id}/asientos")
    public ResponseEntity<EstadoAsientosRemotoDTO> obtenerAsientosEvento(@PathVariable Long id) {
        log.info("🌐 [Proxy] GET /api/proxy/eventos/{}/asientos", id);
        EstadoAsientosRemotoDTO dto = estadoAsientosRedisService.obtenerEstadoAsientos(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * POST /api/proxy/eventos/{id}/venta
     *
     * Recibe una venta desde el backend del alumno y la reenvía a la cátedra (P7).
     * El {id} se usa solo para loguear/coherencia: la cátedra toma el eventoId del body.
     */
    @PostMapping("/eventos/{id}/venta")
    public ResponseEntity<?> crearVentaEvento(
            @PathVariable Long id,
            @RequestBody Map<String, Object> ventaJson
    ) {
        log.info("💸 [Proxy] POST /api/proxy/eventos/{}/venta", id);

        try {
            // Podés loguear un poco el payload si querés:
            log.debug("💸 [Proxy] Payload venta recibido: {}", ventaJson);

            // Delegamos en la cátedra
            catServiceClient.crearVenta(ventaJson);

            log.info("💸 [Proxy] Venta confirmada en la cátedra para evento {}", id);
            // La cátedra responde 200 sin body → devolvemos 200 sin body también
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("💸 [Proxy] Error al crear venta en la cátedra para evento {}", id, e);
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"No se pudo crear la venta en la cátedra\"}");
        }
    }

    /**
     * POST /api/proxy/eventos/{id}/bloqueos
     *
     * Recibe una solicitud de bloqueo de asiento desde el backend del alumno
     * y registra el bloqueo en la cátedra llamando a /api/endpoints/v1/bloquear-asientos.
     *
     * El {id} es el ID del evento en la cátedra (externalId).
     */
    @PostMapping("/eventos/{id}/bloqueos")
    public ResponseEntity<?> crearBloqueoEvento(
            @PathVariable Long id,
            // Recibe el DTO enviado desde el Backend (que ya incluye fila, columna y personaActual/username)
            @RequestBody AsientoRemotoDTO asiento
    ) {
        log.info("🔒 [Proxy] POST /api/proxy/eventos/{}/bloqueos fila={}, columna={}",
                id, asiento.getFila(), asiento.getColumna());

        if (asiento.getFila() == null || asiento.getColumna() == null) {
            return ResponseEntity.badRequest().body("{\"error\":\"fila y columna son obligatorias\"}");
        }

        // --- CONSTRUCCIÓN DEL PAYLOAD PLANO REQUERIDO POR LA CÁTEDRA ---

        Map<String, Object> body = new HashMap<>();
        body.put("eventoId", id);
        body.put("fila", asiento.getFila());
        body.put("columna", asiento.getColumna());

        try {
            // Se envía el payload plano al Feign Client
            Object respuesta = catServiceClient.bloquearAsiento(body);
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            log.error("💥 [Proxy] Error al bloquear asiento en la cátedra para eventoId={}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"No se pudo bloquear el asiento en la cátedra\"}");
        }
    }




}
