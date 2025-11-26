package ar.edu.um.proxyservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración HTTP para el cliente que va a llamar
 * al servidor de la cátedra (CatService).
 *
 * Aquí definimos un bean RestTemplate para que Spring
 * lo pueda inyectar donde lo necesitemos.
 */
@Configuration // Le dice a Spring: “esta clase define beans de configuración”.
public class CatServiceHttpConfig {
    /**
     * Crea y expone un RestTemplate como bean de Spring.
     *
     * RestTemplate es el cliente HTTP síncrono que usaremos
     * para consumir los endpoints de la cátedra.
     *
     * Se uso directamente "new RestTemplate()" para mantener
     * la configuración simple y evitar depender de RestTemplateBuilder.
     */
    @Bean // @Bean RestTemplate: le da a Spring un objeto RestTemplate listo para usar.
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
