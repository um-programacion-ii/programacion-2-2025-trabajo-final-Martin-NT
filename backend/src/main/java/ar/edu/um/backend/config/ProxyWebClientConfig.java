package ar.edu.um.backend.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuración del WebClient usado por el backend para comunicarse con el proxy-service.
 *
 * Este bean se crea una sola vez y luego se inyecta donde se necesite
 * (por ejemplo, en ProxyService).
 */
@Configuration
public class ProxyWebClientConfig {

    private static final Logger log = LoggerFactory.getLogger(ProxyWebClientConfig.class);

    /**
     * Crea y configura el WebClient que usará el backend para llamar al proxy.
     *
     * @param proxyProperties → contiene la baseUrl y el token configurados en application-dev.yml
     * @return WebClient ya configurado y listo para usar.
     */
    @Bean
    public WebClient proxyWebClient(ProxyProperties proxyProperties) {

        // Obtiene la URL base del proxy desde configuración
        String baseUrl = proxyProperties.getBaseUrl();
        String token = proxyProperties.getToken();

        log.info("[Proxy-Config] Usando baseUrl={} para llamadas al proxy", baseUrl);

        // Builder inicial del WebClient
        WebClient.Builder builder = WebClient.builder()
            .baseUrl(baseUrl); // Todas las llamadas usarán esta URL raíz

        // Si el token está definido, agrega el header Authorization en forma automática
        if (token != null && !token.isBlank()) {
            log.info("[Proxy-Config] Configurando Authorization Bearer para llamadas al proxy");
            builder.defaultHeader("Authorization", "Bearer " + token);
        } else {
            // Advertencia si el token falta
            log.warn("[Proxy-Config] PROXY_TOKEN vacío: se llamará al proxy SIN Authorization");
        }

        // Construye el cliente final
        return builder.build();
    }
}
