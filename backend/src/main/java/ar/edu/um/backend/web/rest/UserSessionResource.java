package ar.edu.um.backend.web.rest;

import ar.edu.um.backend.security.SecurityUtils;
import ar.edu.um.backend.service.UserSessionService; // Importamos la Interfaz
import ar.edu.um.backend.service.dto.UserSessionDTO;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for managing User Sessions.
 * Controlador REST para gestionar el estado de la sesión del usuario en Redis.
 */
@RestController
@RequestMapping("/api/session")
public class UserSessionResource {

    private final Logger log = LoggerFactory.getLogger(UserSessionResource.class);

    // Inyectamos la INTERFAZ, Spring buscará automáticamente la implementación (Impl)
    private final UserSessionService userSessionService;

    public UserSessionResource(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    /**
     * Obtiene el username del usuario logueado desde el contexto de seguridad (JWT).
     */
    private String getCurrentUsername() {
        return SecurityUtils
            .getCurrentUserLogin()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión"));
    }

    /**
     * POST /api/session/evento
     * Guarda el ID del evento seleccionado en el estado del usuario.
     */
    @PostMapping("/evento")
    public ResponseEntity<Void> setEvento(@RequestBody Map<String, Long> body) {
        String username = getCurrentUsername();
        Long eventoId = body.get("eventoId");

        log.debug("REST request para guardar evento {} en sesión de {}", eventoId, username);

        // 1. Cargar el estado actual o crear uno nuevo si no existe
        UserSessionDTO session = userSessionService.loadSession(username).orElse(new UserSessionDTO());

        // 2. Actualizar los datos del DTO
        session.setIdEventoSeleccionado(eventoId);
        session.setPasoActual("SELECCION_ASIENTOS");

        // 3. Guardar en Redis usando el servicio
        userSessionService.saveSession(username, session);

        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/session/evento
     * Recupera el ID del evento seleccionado desde Redis.
     */
    @GetMapping("/evento")
    public ResponseEntity<Map<String, Long>> getEvento() {
        String username = getCurrentUsername();
        log.debug("REST request para leer evento de sesión de {}", username);

        Map<String, Long> response = new HashMap<>();

        // 1. Intentar cargar desde Redis
        userSessionService.loadSession(username).ifPresentOrElse(
            session -> response.put("eventoId", session.getIdEventoSeleccionado()),
            () -> response.put("eventoId", null) // Si no hay sesión guardada, devolvemos null
        );

        return ResponseEntity.ok(response);
    }
}
