package ar.edu.um.backend.service;

import ar.edu.um.backend.service.dto.UserSessionDTO;
import java.util.Optional;

/**
 * Service Interface for managing User Session.
 */
public interface UserSessionService {

    /**
     * Guarda la sesión del usuario.
     *
     * @param username el usuario dueño de la sesión.
     * @param session el DTO con los datos a guardar.
     */
    void saveSession(String username, UserSessionDTO session);

    /**
     * Recupera la sesión del usuario.
     *
     * @param username el usuario dueño de la sesión.
     * @return el DTO si existe, o empty si no.
     */
    Optional<UserSessionDTO> loadSession(String username);

    /**
     * Elimina la sesión del usuario.
     *
     * @param username el usuario dueño de la sesión.
     */
    void deleteSession(String username);
}
