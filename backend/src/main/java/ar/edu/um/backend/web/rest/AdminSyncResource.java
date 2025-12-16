package ar.edu.um.backend.web.rest;
import ar.edu.um.backend.security.AuthoritiesConstants;
import ar.edu.um.backend.service.EventoSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
/**
 * Controlador REST encargado de exponer endpoints administrativos.
 *
 * En este caso, administra la sincronización manual de eventos entre:
 *   Backend ←→ Proxy-Service ←→ Servidor de la cátedra.
 *
 * Solo los usuarios con permisos de ADMIN pueden invocar esta operación,
 * ya que modifica la base de datos local.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminSyncResource {
    private static final Logger log = LoggerFactory.getLogger(AdminSyncResource.class);

    /**
     * Servicio encargado de realizar la sincronización completa de eventos.
     * Se inyecta por constructor.
     */
    private final EventoSyncService eventoSyncService;

    public AdminSyncResource(EventoSyncService eventoSyncService) {
        this.eventoSyncService = eventoSyncService;
    }

    /**
     * POST /api/admin/sync-eventos
     * ---------------------------------------------------------
     * Este endpoint:
     *   - Dispara una sincronización manual de eventos.
     *   - Llama internamente a EventoSyncService.sincronizarEventosDesdeProxy().
     *   - Reemplaza datos locales con los datos reales obtenidos desde la cátedra.
     *
     * Seguridad:
     *   @PreAuthorize(...) → Solo ADMIN puede usarlo.
     *
     * Respuesta:
     *   204 No Content → Se ejecutó correctamente pero no devuelve body.
     */
    @PostMapping("/sync-eventos")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> syncEventosManualmente() {

        log.info("[Admin-Sync] Solicitud manual de sincronización de eventos.");

        // Ejecuta la sincronización real (acceso al proxy + persistencia local)
        eventoSyncService.sincronizarEventosDesdeProxy();

        log.info("[Admin-Sync] Sincronización manual finalizada.");

        return ResponseEntity.noContent().build(); // HTTP 204
    }
}
