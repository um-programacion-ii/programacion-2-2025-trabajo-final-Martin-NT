package ar.edu.um.backend.web.rest;
import ar.edu.um.backend.service.EventoSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint que será llamado por el proxy-service para notificar cambios en eventos
 * (por ejemplo, cuando Kafka informa que hubo actualizaciones).
 */
@RestController
@RequestMapping("/api/proxy")
public class ProxyNotificationResource {

    private static final Logger log = LoggerFactory.getLogger(ProxyNotificationResource.class);

    private final EventoSyncService eventoSyncService;

    public ProxyNotificationResource(EventoSyncService eventoSyncService) {
        this.eventoSyncService = eventoSyncService;
    }

    /**
     * POST /api/proxy/notificacion-evento
     *
     * Pensado para ser llamado EXCLUSIVAMENTE por el proxy-service.
     * Por ahora:
     *  - Loguea el contenido recibido (si viene algo en el body).
     *  - Dispara nuevamente la sincronización de eventos contra el proxy.
     */
    @PostMapping("/notificacion-evento")
    public ResponseEntity<String> recibirNotificacionDesdeProxy(@RequestBody(required = false) String body) {
        log.info("[Proxy-Backend] Notificación recibida desde proxy en /api/proxy/notificacion-evento");

        if (body != null && !body.isBlank()) {
            log.info("[Proxy-Backend] Body de la notificación: {}", body);
        } else {
            log.info("[Proxy-Backend] Notificación sin body (se procede igual con la sincronización).");
        }

        // Disparar la sincronización
        eventoSyncService.sincronizarEventosDesdeProxy();

        return ResponseEntity.ok("{\"status\":\"ok\",\"mensaje\":\"Notificación procesada y sincronización disparada\"}");
    }
}
