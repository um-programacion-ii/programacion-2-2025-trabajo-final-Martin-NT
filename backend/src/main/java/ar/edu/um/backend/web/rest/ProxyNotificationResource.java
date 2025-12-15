package ar.edu.um.backend.web.rest;

import ar.edu.um.backend.service.EventoSyncService;
import ar.edu.um.backend.service.VentaSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints que serán llamados por el proxy-service para notificar cambios:
 *  - en EVENTOS (cuando Kafka informa que hubo actualizaciones de eventos/asientos),
 *  - en VENTAS (cuando Kafka informa que una venta fue confirmada o rechazada).
 */
@RestController
@RequestMapping("/api/proxy")
public class ProxyNotificationResource {

    private static final Logger log = LoggerFactory.getLogger(ProxyNotificationResource.class);

    private final EventoSyncService eventoSyncService;
    private final VentaSyncService ventaSyncService;

    public ProxyNotificationResource(
        EventoSyncService eventoSyncService,
        VentaSyncService ventaSyncService
    ) {
        this.eventoSyncService = eventoSyncService;
        this.ventaSyncService = ventaSyncService;
    }

    /**
     * POST /api/proxy/notificacion-evento
     *
     * Pensado para ser llamado EXCLUSIVAMENTE por el proxy-service.
     *
     * Comportamiento actual:
     *  - Loguea el contenido recibido (si viene algo en el body).
     *  - Dispara nuevamente la sincronización de eventos/asientos contra el proxy.
     */
    @PostMapping("/notificacion-evento")
    public ResponseEntity<String> recibirNotificacionEvento(@RequestBody(required = false) String body) {
        log.info("[Proxy-Backend] Notificación de EVENTO recibida desde proxy en /api/proxy/notificacion-evento");

        if (body != null && !body.isBlank()) {
            log.info("[Proxy-Backend] Body de la notificación de evento: {}", body);
        } else {
            log.info("[Proxy-Backend] Notificación de evento sin body (se procede igual con la sincronización).");
        }

        // Disparar la sincronización de eventos/asientos
        eventoSyncService.sincronizarEventosDesdeProxy();

        return ResponseEntity.ok(
            "{\"status\":\"ok\",\"mensaje\":\"Notificación de evento procesada y sincronización disparada\"}"
        );
    }

    /**
     * POST /api/proxy/notificacion-venta
     *
     * Endpoint para notificaciones de VENTAS enviadas por el proxy-service
     * (normalmente originadas en Kafka en el servidor de la cátedra).
     *
     * Comportamiento actual:
     *  - Loguea el body si viene.
     *  - Delegar el procesamiento en VentaSyncService.procesarNotificacionVenta(body),
     *    donde se parsea el JSON al formato ProxyVentaDTO y se actualiza la
     *    venta local (estado, descripción, etc.) usando ventaId como externalId.
     */
    @PostMapping("/notificacion-venta")
    public ResponseEntity<String> recibirNotificacionVenta(@RequestBody(required = false) String body) {
        log.info("[Proxy-Backend] Notificación de VENTA recibida desde proxy en /api/proxy/notificacion-venta");

        if (body != null && !body.isBlank()) {
            log.info("[Proxy-Backend] Body de la notificación de venta: {}", body);
            // Delegamos el procesamiento al servicio de sincronización de ventas
            ventaSyncService.procesarNotificacionVenta(body);
        } else {
            log.warn("[Proxy-Backend] Notificación de venta sin body. No hay datos para procesar.");
        }

        return ResponseEntity.ok(
            "{\"status\":\"ok\",\"mensaje\":\"Notificación de venta recibida\"}"
        );
    }
}
