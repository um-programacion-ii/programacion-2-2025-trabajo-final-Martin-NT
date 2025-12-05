package ar.edu.um.proxyservice.config;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;

/**
 * Filtro simple de autenticación para el proxy.
 *
 * Reglas:
 * - Requiere header Authorization: Bearer <token>
 * - No valida el JWT (solo verifica presencia)
 * - Si existe → crea un usuario "proxy-user" para marcar la request como autenticada
 * - Si no existe → devuelve 403
 */
public class ProxyTokenAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ProxyTokenAuthFilter.class);

    // NO ejecutar este filtro para /actuator/**
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Solo protegemos /api/proxy/**
        if (!path.startsWith("/api/proxy")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Validación del header Authorization
        if (authHeader == null || !authHeader.startsWith("Bearer ") || authHeader.length() <= 7) {
            log.warn("🛡️  [Seguridad] Acceso bloqueado a {} {}: falta header Authorization Bearer",
                    request.getMethod(), path);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Debe enviar un token Bearer");
            return;
        }

        // Extrae token sin validarlo
        String token = authHeader.substring(7);

        // Para logs: mostramos solo fragmento para no exponerlo
        String tokenPreview = token.length() > 12 ? token.substring(0, 12) + "..." : token;
        log.debug("🛡️  [Seguridad] Token Bearer presente (parcial={} , longitud={})", tokenPreview, token.length());

        // Simular usuario autenticado (requerido por .authenticated())
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "proxy-user",                 // usuario simbólico
                        null,                         // sin credenciales
                        Collections.emptyList()       // sin roles
                );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Continúa con la request
        filterChain.doFilter(request, response);
    }
}
