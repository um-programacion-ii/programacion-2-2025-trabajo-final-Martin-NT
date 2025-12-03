package ar.edu.um.proxyservice.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Desactiva CSRF (es un API, no formulario)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()  // Permitir todo sin autenticación (temporal)
                )
                .httpBasic(basic -> basic.disable()) // Desactivar Basic Auth
                .formLogin(form -> form.disable());  // Desactivar Login por formularios

        return http.build();
    }
}
