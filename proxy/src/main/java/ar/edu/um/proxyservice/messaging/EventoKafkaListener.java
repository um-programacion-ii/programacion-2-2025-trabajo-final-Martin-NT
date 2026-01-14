package ar.edu.um.proxyservice.messaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Hace que Spring lo detecte automáticamente como bean.
// Sin esto, no se registra en el contenedor → no escucha Kafka.
@Component
public class EventoKafkaListener {
    private static final Logger log = LoggerFactory.getLogger(EventoKafkaListener.class);

    // Esta anotación le dice al proxy:
    @KafkaListener(
            topics = "eventos-actualizacion", // A qué topic escuchar → eventos-actualizacion
            groupId = "${PROXY_GROUP_ID:grupo-alumno}" // Con qué groupId
    )
    // Qué hacer cuando llega un mensaje → ejecutar onEventoActualizado
    public void onEventoActualizado(String mensaje) {
        log.info("📡 [Kafka] Mensaje recibido en eventos-actualizacion");
        log.info("📡 [Kafka] Payload recibido: {}", mensaje);
    }
}

