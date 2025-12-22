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
import org.springframework.web.server.ResponseStatusException;

@Aspect
public class LoggingAspect {

    private final Environment env;

    public LoggingAspect(Environment env) {
        this.env = env;
    }

    @Pointcut(
        "within(@org.springframework.stereotype.Repository *)" +
            " || within(@org.springframework.stereotype.Service *)" +
            " || within(@org.springframework.web.bind.annotation.RestController *)"
    )
    public void springBeanPointcut() {}

    @Pointcut(
        "within(ar.edu.um.backend.repository..*)" +
            " || within(ar.edu.um.backend.service..*)" +
            " || within(ar.edu.um.backend.web.rest..*)"
    )
    public void applicationPackagePointcut() {}

    private Logger logger(JoinPoint joinPoint) {
        return LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringTypeName());
    }

    @AfterThrowing(pointcut = "applicationPackagePointcut() && springBeanPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        Logger log = logger(joinPoint);

        // 1) Errores de negocio / validaciones -> sin stacktrace
        if (e instanceof IllegalStateException || e instanceof IllegalArgumentException) {
            log.warn("[Negocio] {}() falló: {}", joinPoint.getSignature().getName(), e.getMessage());
            return;
        }

        // 1.5) Problemas de datos (ej: enums inválidos / mapping) -> sin stacktrace
        if (e instanceof org.springframework.dao.InvalidDataAccessApiUsageException) {
            log.warn("[Datos] {}() falló: {}", joinPoint.getSignature().getName(), e.getMessage());
            return;
        }

        // 2) Excepciones HTTP 4xx -> sin stacktrace
        if (e instanceof ResponseStatusException rse && rse.getStatusCode().is4xxClientError()) {
            log.warn("{}() -> {}", joinPoint.getSignature().getName(), rse.getMessage());
            return;
        }

        // 3) Errores reales -> stacktrace SOLO si DEBUG
        if (log.isDebugEnabled()) {
            log.error(
                "Exception in {}() with cause='{}' and exception='{}'",
                joinPoint.getSignature().getName(),
                e.getCause() != null ? e.getCause() : "NULL",
                e.getMessage(),
                e
            );
        } else {
            log.error(
                "Exception in {}() -> {} (cause={})",
                joinPoint.getSignature().getName(),
                e.getMessage(),
                e.getCause() != null ? e.getCause() : "NULL"
            );
        }
    }

    @Around("applicationPackagePointcut() && springBeanPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = logger(joinPoint);

        if (log.isDebugEnabled()) {
            log.debug("Enter: {}() args={}", joinPoint.getSignature().getName(), Arrays.toString(joinPoint.getArgs()));
        }

        Object result = joinPoint.proceed();

        if (log.isDebugEnabled()) {
            log.debug("Exit: {}() result={}", joinPoint.getSignature().getName(), result);
        }

        return result;
    }
}
