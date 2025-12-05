package ar.edu.um.proxyservice.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    /**
     * Bean del filtro personalizado que valida que exista un header:
     * Authorization: Bearer <token>
     * - Este filtro NO valida el JWT.
     * - Solo revisa que exista el header y deja pasar la request.
     */
    @Bean
    public ProxyTokenAuthFilter proxyTokenAuthFilter() {
        return new ProxyTokenAuthFilter();
    }

    // Configuración principal de seguridad del proxy.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // No usamos formularios web → CSRF se desactiva
                .csrf(csrf -> csrf.disable())

                // API sin sesiones → cada request debe traer su propio token
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Reglas de autorización por rutas
                .authorizeHttpRequests(auth -> auth

                        // Actuator (health/info) abierto para diagnosticar en dev
                        .requestMatchers("/actuator/**").permitAll()

                        // El proxy sí requiere autenticación → se debe enviar Bearer <token>
                        .requestMatchers("/api/proxy/**").authenticated()

                        // Cualquier otra ruta queda bloqueada
                        .anyRequest().denyAll()
                )

                // Se inserta el filtro ANTES del filtro estándar de Spring Security para que intercepte el Authorization Bearer
                .addFilterBefore(proxyTokenAuthFilter(), UsernamePasswordAuthenticationFilter.class)

                // El proxy NO usa Basic Auth
                .httpBasic(basic -> basic.disable())

                // No hay login por formularios
                .formLogin(form -> form.disable());

        return http.build();
    }
}
