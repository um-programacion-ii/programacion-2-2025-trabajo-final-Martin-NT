package ar.edu.um.backend.service;
import ar.edu.um.backend.domain.Evento;
import ar.edu.um.backend.repository.EventoRepository;
import ar.edu.um.backend.service.dto.AsientoBloqueoRequestDTO;
import ar.edu.um.backend.service.dto.AsientoBloqueoResponseDTO;
import ar.edu.um.backend.service.dto.AsientoEstadoDTO;
import ar.edu.um.backend.service.dto.AsientoUbicacionDTO;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de negocio del backend encargado de gestionar el bloqueo de asientos.
 *
 * Flujo:
 * 1) Valida el evento local (existencia, activo, filas/columnas).
 * 2) Valida las posiciones solicitadas (fila/columna).
 * 3) Realiza un pre-chequeo local del estado de los asientos
 *    (vendido / bloqueado vigente).
 * 4) Si todos son bloqueables, delega el bloqueo real al proxy-service
 *    (Redis / cátedra).
 *
 * Importante:
 * - El proxy/cátedra es la fuente de verdad del bloqueo.
 * - El TTL del bloqueo NO se maneja aquí.
 * - La operación es atómica ("todo o nada").
 */
@Service
@Transactional
public class AsientoBloqueoService {

    private static final Logger log = LoggerFactory.getLogger(AsientoBloqueoService.class);

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

    public AsientoBloqueoResponseDTO bloquearAsientos(Long eventoIdLocal, AsientoBloqueoRequestDTO request) {

        if (eventoIdLocal == null) {
            throw new IllegalStateException("eventoIdLocal requerido.");
        }
        if (request == null || request.getAsientos() == null || request.getAsientos().isEmpty()) {
            throw new IllegalStateException("Debe enviar al menos un asiento.");
        }

        // 1) Obtener evento local
        Evento evento = eventoRepository.findById(eventoIdLocal)
            .orElseThrow(() ->
                new IllegalStateException("Evento no encontrado idLocal=" + eventoIdLocal)
            );

        // 2) Validaciones del evento
        if (Boolean.FALSE.equals(evento.getActivo())) {
            throw new IllegalStateException("El evento está inactivo, no se pueden bloquear asientos.");
        }
        if (evento.getFilaAsientos() == null || evento.getColumnaAsientos() == null) {
            throw new IllegalStateException("El evento no tiene configuradas filas/columnas.");
        }
        if (evento.getExternalId() == null) {
            throw new IllegalStateException("El evento no tiene externalId (id cátedra).");
        }

        Integer maxFilas = evento.getFilaAsientos();
        Integer maxColumnas = evento.getColumnaAsientos();

        log.info(
            "🔒 [Bloqueo] Solicitud recibida: eventoLocal={}, externalId={}, cantidadAsientos={}",
            eventoIdLocal,
            evento.getExternalId(),
            request.getAsientos().size()
        );

        // 3) Validaciones por asiento (rango)
        for (AsientoUbicacionDTO a : request.getAsientos()) {
            if (a == null || a.getFila() == null || a.getColumna() == null) {
                throw new IllegalStateException("Cada asiento debe incluir fila y columna.");
            }
            if (a.getFila() < 1 || a.getColumna() < 1) {
                throw new IllegalStateException("Fila y columna deben ser >= 1.");
            }
            if (a.getFila() > maxFilas || a.getColumna() > maxColumnas) {
                throw new IllegalStateException(
                    "Asiento (" + a.getFila() + "," + a.getColumna() + ") fuera de rango."
                );
            }
        }

        // 4) Pre-chequeo de estados locales/remotos
        List<AsientoEstadoDTO> detalle = new ArrayList<>();
        boolean todosBloqueables = true;

        for (AsientoUbicacionDTO a : request.getAsientos()) {

            AsientoEstadoDTO estado =
                asientoEstadoService.obtenerEstadoAsiento(eventoIdLocal, a.getFila(), a.getColumna());

            String est = (estado != null && estado.getEstado() != null)
                ? estado.getEstado()
                : "DESCONOCIDO";

            if ("VENDIDO".equalsIgnoreCase(est) || "OCUPADO".equalsIgnoreCase(est)) {
                todosBloqueables = false;
                detalle.add(new AsientoEstadoDTO(a.getFila(), a.getColumna(), "Ocupado"));
            } else if ("BLOQUEADO_VIGENTE".equalsIgnoreCase(est) || "BLOQUEADO".equalsIgnoreCase(est)) {
                todosBloqueables = false;
                detalle.add(new AsientoEstadoDTO(a.getFila(), a.getColumna(), "Bloqueado"));
            }
        }

        // 5) Si alguno no es bloqueable → respuesta negativa
        if (!todosBloqueables) {
            AsientoBloqueoResponseDTO resp = new AsientoBloqueoResponseDTO();
            resp.setResultado(false);
            resp.setDescripcion("No todos los asientos pueden ser bloqueados");
            resp.setEventoId(evento.getExternalId());
            resp.setAsientos(detalle);

            log.warn("⛔ [Bloqueo] Rechazado por pre-chequeo local (eventoLocal={})", eventoIdLocal);
            return resp;
        }

        // 6) Delegar bloqueo real al proxy (fuente de verdad)
        AsientoBloqueoRequestDTO requestCat = new AsientoBloqueoRequestDTO();
        requestCat.setEventoId(evento.getExternalId());
        requestCat.setAsientos(request.getAsientos());

        AsientoBloqueoResponseDTO resp = proxyService.crearBloqueoEnProxy(requestCat);

        log.info(
            "✅ [Bloqueo] Respuesta proxy: resultado={}, eventoId={}",
            resp.isResultado(),
            resp.getEventoId()
        );

        return resp;
    }
}
