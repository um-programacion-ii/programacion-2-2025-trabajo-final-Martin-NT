package ar.edu.um.backend.config;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Clase que mapea propiedades externas definidas en application-dev.yml
 * bajo el prefijo "proxy".
 *
 * Spring inyecta automáticamente estos valores en esta clase.
 */
@ConfigurationProperties(prefix = "proxy")
public class ProxyProperties {

    /**
     * URL base del proxy del alumno.
     * Se usa como root para construir todas las llamadas HTTP desde el backend.
     */
    private String baseUrl;

    /**
     * Token JWT que el backend enviará al proxy en cada request.
     * Este valor proviene de la variable de entorno PROXY_TOKEN.
     * El proxy validará este token mediante ProxyTokenAuthFilter.
     */
    private String token;

    // Getters / Setters
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
