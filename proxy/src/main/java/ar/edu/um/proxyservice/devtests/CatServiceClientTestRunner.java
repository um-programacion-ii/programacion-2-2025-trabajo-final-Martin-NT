package ar.edu.um.proxyservice.devtests;
import ar.edu.um.proxyservice.service.CatServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Runner temporal para probar el cliente HTTP hacia la cátedra (CatServiceClient).
 *
 * Se ejecuta automáticamente al iniciar el proxy en perfil "dev"
 * y llama a los métodos del cliente, logueando los resultados.
 */
//@Component
//@Profile("dev")
public class CatServiceClientTestRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatServiceClientTestRunner.class);

    private final CatServiceClient catServiceClient;

    public CatServiceClientTestRunner(CatServiceClient catServiceClient) {
        this.catServiceClient = catServiceClient;
    }

    @Override
    public void run(String... args) {
        log.info("=== [CatServiceClientTestRunner] PROBANDO CatServiceClient (eventos) ===");

        try {
            String resumenes = catServiceClient.listarEventosResumidos();
            log.info("[DevTest] listarEventosResumidos() -> bodyLength={}", resumenes != null ? resumenes.length() : null);
        } catch (Exception e) {
            log.error("[DevTest] Error en listarEventosResumidos()", e);
        }

        try {
            String completos = catServiceClient.listarEventosCompletos();
            log.info("[DevTest] listarEventosCompletos() -> bodyLength={}", completos != null ? completos.length() : null);
        } catch (Exception e) {
            log.error("[DevTest] Error en listarEventosCompletos()", e);
        }

        try {
            Long eventoId = 1L; // o alguno que sepas que exista
            String evento = catServiceClient.obtenerEventoPorId(eventoId);
            log.info("[DevTest] obtenerEventoPorId({}) -> bodyLength={}", eventoId, evento != null ? evento.length() : null);
        } catch (Exception e) {
            log.error("[DevTest] Error en obtenerEventoPorId()", e);
        }

        try {
            String forzar = catServiceClient.forzarActualizacion();
            log.info("[DevTest] forzarActualizacion() -> bodyLength={}", forzar != null ? forzar.length() : null);
        } catch (Exception e) {
            log.error("[DevTest] Error en forzarActualizacion()", e);
        }

        log.info("=== [CatServiceClientTestRunner] FIN PRUEBA CatServiceClient ===");
    }
}
