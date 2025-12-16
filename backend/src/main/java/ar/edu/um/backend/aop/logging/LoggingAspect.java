package ar.edu.um.backend.aop.logging;

import java.util.Arrays;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.server.ResponseStatusException;
import tech.jhipster.config.JHipsterConstants;

/**
 * Aspecto para el logging de ejecución de componentes Service, Repository y RestController.
 *
 * Evita mostrar stacktraces innecesarios en errores controlados (ej. 404).
 * Solo muestra trazas completas en errores reales del servidor (5xx).
 */
@Aspect
public class LoggingAspect {

    private final Environment env;

    public LoggingAspect(Environment env) {
        this.env = env;
    }

    /**
     * Pointcut que aplica a todos los repositorios, servicios y controladores REST.
     */
    @Pointcut(
        "within(@org.springframework.stereotype.Repository *)" +
            " || within(@org.springframework.stereotype.Service *)" +
            " || within(@org.springframework.web.bind.annotation.RestController *)"
    )
    public void springBeanPointcut() {}

    /**
     * Pointcut que aplica a los paquetes principales de la app.
     */
    @Pointcut(
        "within(ar.edu.um.backend.repository..*)" +
            " || within(ar.edu.um.backend.service..*)" +
            " || within(ar.edu.um.backend.web.rest..*)"
    )
    public void applicationPackagePointcut() {}

    /**
     * Obtiene el logger del joinPoint actual.
     */
    private Logger logger(JoinPoint joinPoint) {
        return LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringTypeName());
    }

    /**
     * Logging de excepciones lanzadas.
     * Evita mostrar stacktrace completo en errores controlados (ej. 404).
     */
    @AfterThrowing(pointcut = "applicationPackagePointcut() && springBeanPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        Logger log = logger(joinPoint);

        // 1) Errores de negocio (validaciones) → sin stacktrace
        if (e instanceof IllegalStateException || e instanceof IllegalArgumentException) {
            log.warn(
                "⚠️  [Negocio] {}() falló: {}",
                joinPoint.getSignature().getName(),
                e.getMessage()
            );
            return;
        }

        // 2) Excepciones HTTP 4xx (ej: ResponseStatusException 400/404) → sin stacktrace
        if (e instanceof ResponseStatusException rse && rse.getStatusCode().is4xxClientError()) {
            log.warn(
                "⚠️  {}() -> {}",
                joinPoint.getSignature().getName(),
                rse.getMessage()
            );
            return;
        }

        // 3) Errores "reales" del servidor (bugs) → acá sí queremos ver bien qué pasó
        if (env.acceptsProfiles(Profiles.of(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT))) {
            log.error(
                "❌ Exception in {}() with cause='{}' and exception='{}'",
                joinPoint.getSignature().getName(),
                e.getCause() != null ? e.getCause() : "NULL",
                e.getMessage(),
                e // acá sí dejamos el stacktrace completo para debug
            );
        } else {
            log.error(
                "❌ Exception in {}() with cause={}",
                joinPoint.getSignature().getName(),
                e.getCause() != null ? e.getCause() : "NULL"
            );
        }
    }


    /**
     * Logging de entrada/salida de métodos (solo si DEBUG activo).
     */
    @Around("applicationPackagePointcut() && springBeanPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = logger(joinPoint);
        if (log.isDebugEnabled()) {
            log.debug("➡️  Enter: {}() with args={}", joinPoint.getSignature().getName(), Arrays.toString(joinPoint.getArgs()));
        }

        Object result = joinPoint.proceed();

        if (log.isDebugEnabled()) {
            log.debug("⬅️  Exit: {}() with result={}", joinPoint.getSignature().getName(), result);
        }

        return result;
    }

}
