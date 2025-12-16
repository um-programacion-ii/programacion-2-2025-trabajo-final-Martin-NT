package ar.edu.um.backend.service;
import ar.edu.um.backend.domain.Evento;
import ar.edu.um.backend.repository.EventoRepository;
import ar.edu.um.backend.service.dto.AsientoBloqueoRequestDTO;
import ar.edu.um.backend.service.dto.AsientoBloqueoResponseDTO;
import ar.edu.um.backend.service.dto.AsientoEstadoDTO;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Servicio de negocio para bloquear asientos con duración fija (5 minutos),
 * consultando siempre el estado real en Redis (vía proxy).
 */
@Service
@Transactional
public class AsientoBloqueoService {
    private static final Logger log = LoggerFactory.getLogger(AsientoBloqueoService.class);
    private static final long DURACION_BLOQUEO_MINUTOS = 5L;

    private final EventoRepository eventoRepository;
    private final AsientoEstadoService asientoEstadoService;
    private final ProxyService proxyService;

    public AsientoBloqueoService(
        EventoRepository eventoRepository,
        AsientoEstadoService asientoEstadoService,
        ProxyService proxyService
    ) {
        this.eventoRepository = eventoRepository;
        this.asientoEstadoService = asientoEstadoService;
        this.proxyService = proxyService;
    }

    /**
     * Bloquea un asiento para un evento local.
     *
     * Reglas:
     * - fila / columna >= 1
     * - dentro del rango definido por el Evento (filas/columnas máximas)
     * - el evento debe estar activo
     * - no se bloquea si está vendido
     * - no se bloquea si ya está bloqueado vigente
     * - si el bloqueo expiró, se permite bloquear de nuevo
     */
    public AsientoBloqueoResponseDTO bloquearAsiento(Long eventoIdLocal, AsientoBloqueoRequestDTO request) {
        Integer fila = request.getFila();
        Integer columna = request.getColumna();

        log.info("🔒 [Bloqueo] Solicitud de bloqueo: eventoIdLocal={}, fila={}, columna={}",
            eventoIdLocal, fila, columna);

        // --- Validación básica fila/columna ---
        if (fila == null || columna == null || fila < 1 || columna < 1) {
            log.warn("⚠️ [Bloqueo] Intento de bloquear asiento con fila/columna inválida: fila={}, columna={}", fila, columna);
            throw new IllegalStateException("Fila y columna deben ser mayores o iguales a 1.");
        }

        // --- Buscar evento local ---
        Evento evento = eventoRepository
            .findById(eventoIdLocal)
            .orElseThrow(() -> new IllegalStateException("Evento no encontrado idLocal=" + eventoIdLocal));

        if (Boolean.FALSE.equals(evento.getActivo())) {
            log.warn("⛔ [Bloqueo] Evento inactivo idLocal={}, bloqueo prohibido", eventoIdLocal);
            throw new IllegalStateException("El evento está inactivo, no se pueden bloquear asientos.");
        }

        // --- Validar rango según configuración de asientos del evento ---
        if (fila > evento.getFilaAsientos() || columna > evento.getColumnaAsientos()) {
            log.warn(
                "⚠️ [Bloqueo] Intento de bloquear asiento fuera de rango: eventoIdLocal={}, fila={}, columna={}, maxFilas={}, maxColumnas={}",
                eventoIdLocal, fila, columna, evento.getFilaAsientos(), evento.getColumnaAsientos()
            );
            throw new IllegalStateException("Asiento (" + fila + "," + columna + ") fuera de rango para este evento.");
        }

        // --- Consultar estado real del asiento en Redis (vía AsientoEstadoService) ---
        AsientoEstadoDTO estadoDTO = asientoEstadoService.obtenerEstadoAsiento(eventoIdLocal, fila, columna);
        if (estadoDTO == null) {
            log.warn("⚠️ [Bloqueo] Asiento ({},{}) no encontrado en mapa de estado para eventoIdLocal={}",
                fila, columna, eventoIdLocal);
            throw new IllegalStateException("Asiento (" + fila + "," + columna + ") no es válido para este evento.");
        }

        String estadoActual = estadoDTO.getEstado(); // "LIBRE", "VENDIDO", "BLOQUEADO_VIGENTE", "BLOQUEADO_EXPIRADO", etc.

        // --- Asiento vendido ---
        if ("VENDIDO".equalsIgnoreCase(estadoActual)) {
            log.warn("⛔ [Bloqueo] Asiento ({},{}) vendido, bloqueo prohibido", fila, columna);
            throw new IllegalStateException("El asiento (" + fila + "," + columna + ") ya está vendido.");
        }

        // --- Asiento bloqueado vigente ---
        if ("BLOQUEADO_VIGENTE".equalsIgnoreCase(estadoActual)) {
            log.warn("🔒 [Bloqueo] Asiento ({},{}) ya bloqueado vigente", fila, columna);
            throw new IllegalStateException("El asiento (" + fila + "," + columna + ") ya está bloqueado vigente.");
        }

        // --- Bloqueo expirado (se permite volver a bloquear) ---
        if ("BLOQUEADO_EXPIRADO".equalsIgnoreCase(estadoActual)) {
            log.info("🕒 [Bloqueo] Bloqueo expirado para asiento ({},{}), se permite bloquear nuevamente", fila, columna);
        }

        // --- Registrar bloqueo real llamando al proxy ---
        Long externalId = evento.getExternalId();

        //
        proxyService.crearBloqueoEnProxy(externalId, fila, columna);

        // --- Armar respuesta al frontend ---
        AsientoBloqueoResponseDTO response = new AsientoBloqueoResponseDTO();
        response.setFila(fila);
        response.setColumna(columna);
        response.setEstado("BLOQUEADO");
        response.setExpiraA(LocalDateTime.now().plusMinutes(DURACION_BLOQUEO_MINUTOS));

        log.info("⏳ [Bloqueo] Bloqueo creado para asiento ({},{}), expira a las {}",
            fila, columna, response.getExpiraA());

        return response;
    }
}
