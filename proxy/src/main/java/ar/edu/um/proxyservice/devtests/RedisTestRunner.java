package ar.edu.um.proxyservice.devtests;
import ar.edu.um.proxyservice.service.EstadoAsientosRedisService;
import ar.edu.um.proxyservice.service.dto.EstadoAsientosRemotoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Runner temporal de pruebas para verificar que:
 *  - El proxy se conecta correctamente al Redis remoto.
 *  - La key evento_{ID} existe o no.
 *  - El JSON crudo se lee correctamente.
 *  - El JSON se parsea sin romper la aplicación.
 *
 */
//@Component
//@Profile("dev")
public class RedisTestRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RedisTestRunner.class);

    private final EstadoAsientosRedisService estadoAsientosRedisService;

    public RedisTestRunner(EstadoAsientosRedisService estadoAsientosRedisService) {
        this.estadoAsientosRedisService = estadoAsientosRedisService;
    }

    @Override
    public void run(String... args) {
        Long eventoId = 1L; //  Probar otros ID para ver diferentes resultados

        log.info("=== [RedisTestRunner] PROBANDO LECTURA DE REDIS PARA eventoId={} ===", eventoId);

        // 1. JSON crudo desde Redis
        String jsonCrudo = estadoAsientosRedisService.obtenerEstadoAsientosRaw(eventoId);
        log.info("[DevTest] JSON crudo desde Redis = {}", jsonCrudo);

        // 2. DTO parseado
        EstadoAsientosRemotoDTO dto = estadoAsientosRedisService.obtenerEstadoAsientos(eventoId);
        log.info("[DevTest] DTO parseado -> eventoId={}, asientos={}",
                dto.getEventoId(),
                dto.getAsientos().size());

        log.info("=== [RedisTestRunner] FIN DE PRUEBA DE REDIS ===");
    }
}
