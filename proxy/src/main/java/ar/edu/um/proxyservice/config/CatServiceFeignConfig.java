package ar.edu.um.proxyservice.config;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * Configuración Feign para llamar al servicio de la cátedra.
 * Agrega el header Authorization: Bearer <token> en todas las requests
 * y habilita logs detallados de Feign.
 */
@Configuration
public class CatServiceFeignConfig {

    private static final Logger log = LoggerFactory.getLogger(CatServiceFeignConfig.class);

    // El proxy SIEMPRE llama a la cátedra con el token fijo catedra.jwt-token (el que venga por .env)
    @Value("${catedra.jwt-token:}")
    private String catedraJwtToken;

    @Bean
    public RequestInterceptor catedraAuthRequestInterceptor() {
        return (RequestTemplate template) -> {
            if (catedraJwtToken == null || catedraJwtToken.isBlank()) {
                log.warn("[CatServiceFeignConfig] catedra.jwt-token está vacío. Se llamará sin Authorization.");
                return;
            }

            // Si ya hay un Authorization explícito, no lo piso
            if (!template.headers().containsKey("Authorization")) {
                template.header("Authorization", "Bearer " + catedraJwtToken);
            }
        };
    }

    @Bean
    public feign.Logger.Level feignLoggerLevel() {
        // FULL → loguea request y response completos
        return feign.Logger.Level.FULL;
    }
}
