package ar.edu.um.backend.service.impl;

import ar.edu.um.backend.service.UserSessionService;
import ar.edu.um.backend.service.dto.UserSessionDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserSessionServiceImpl implements UserSessionService {

    private final Logger log = LoggerFactory.getLogger(UserSessionServiceImpl.class);

    // Esta es la herramienta de Spring Boot que se conecta a tu Redis (localhost:6379)
    private final StringRedisTemplate redisTemplate;

    // Usamos ObjectMapper para convertir Objeto <-> JSON (viene listo en JHipster)
    private final ObjectMapper objectMapper;

    private final long tiempoExpiracionSesion;

    private static final String SESSION_KEY_PREFIX = "user:session:";

    public UserSessionServiceImpl(
        StringRedisTemplate redisTemplate,
        ObjectMapper objectMapper,
        // Lee lo que esta en application-dev.yml en caso de no haber nada usa lo preterminado aca, que es 30min
        @Value("${app.session-timeout-minutes:30}") long tiempoExpiracionSesion
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.tiempoExpiracionSesion = tiempoExpiracionSesion;
    }

    private String getKey(String username) {
        return SESSION_KEY_PREFIX + username;
    }

    @Override
    public void saveSession(String username, UserSessionDTO session) {
        log.debug("Guardando sesión para usuario: {}", username);
        String key = getKey(username);

        try {
            // 1. Convertimos el Objeto DTO a texto JSON
            String jsonValue = objectMapper.writeValueAsString(session);

            // 2. Guardamos el texto en Redis
            redisTemplate.opsForValue().set(key, jsonValue, tiempoExpiracionSesion, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.error("Error al convertir la sesión a JSON", e);
            throw new RuntimeException("No se pudo guardar la sesión", e);
        }
    }

    @Override
    public Optional<UserSessionDTO> loadSession(String username) {
        log.debug("Cargando sesión para usuario: {}", username);
        String key = getKey(username);

        // 1. Obtenemos el texto JSON de Redis
        String jsonValue = redisTemplate.opsForValue().get(key);

        if (jsonValue != null) {
            try {
                // 2. Convertimos el texto JSON de vuelta a Objeto DTO
                UserSessionDTO session = objectMapper.readValue(jsonValue, UserSessionDTO.class);
                return Optional.of(session);
            } catch (JsonProcessingException e) {
                log.error("Error al leer la sesión desde JSON", e);
            }
        }
        return Optional.empty();
    }

    @Override
    public void deleteSession(String username) {
        log.debug("Borrando sesión para usuario: {}", username);
        String key = getKey(username);
        redisTemplate.delete(key);
    }
}
