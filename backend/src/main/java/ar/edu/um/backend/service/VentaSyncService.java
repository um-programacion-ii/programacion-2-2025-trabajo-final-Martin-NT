package ar.edu.um.backend.service;
import ar.edu.um.backend.domain.Asiento;
import ar.edu.um.backend.domain.Evento;
import ar.edu.um.backend.domain.Venta;
import ar.edu.um.backend.domain.enumeration.AsientoEstado;
import ar.edu.um.backend.domain.enumeration.VentaEstado;
import ar.edu.um.backend.repository.AsientoRepository;
import ar.edu.um.backend.repository.EventoRepository;
import ar.edu.um.backend.repository.VentaRepository;
import ar.edu.um.backend.service.dto.AsientoEstadoDTO;
import ar.edu.um.backend.service.dto.ProxyVentaRequestDTO;
import ar.edu.um.backend.service.dto.ProxyVentaResponseDTO;
import ar.edu.um.backend.service.dto.VentaAsientoFrontendDTO;
import ar.edu.um.backend.service.dto.VentaRequestFrontendDTO;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Servicio encargado de procesar una venta real de entradas para un evento.
 *
 * RESPONSABILIDAD GENERAL
 * Este servicio coordina la venta de entradas entre:
 *  - El estado REAL de los asientos (Redis / Proxy / Cátedra)
 *  - La validación de reglas de negocio locales
 *  - La persistencia final de la venta y los asientos vendidos en la BD local
 *
 * USO DE IDs
 * - idLocal:
 *     Es el ID del evento en la base de datos local del alumno.
 *     Se usa para:
 *       - Consultar entidades JPA (Evento, Asiento, Venta)
 *       - Construir el mapa de asientos local
 *       - Persistir ventas y marcar asientos como VENDIDO
 *
 * - externalId:
 *     Es el ID del evento en el sistema de la cátedra.
 *     Se usa para:
 *       - Consultar estados de asientos vía Proxy (Redis)
 *       - Enviar la venta real a la cátedra (Payload 7)
 *
 * FLUJO GENERAL DE VENTA
 * ------------------------------------------------------------
 * 1) El frontend envía una solicitud de venta usando externalId.
 * 2) El controller resuelve el evento local y delega a este servicio.
 * 3) El service valida:
 *      - Evento activo
 *      - Evento sincronizado (externalId presente)
 *      - Asientos BLOQUEADO_VIGENTE en Redis
 * 4) Se confirma la venta contra la cátedra vía Proxy.
 * 5) Si la cátedra confirma:
 *      - Se persiste la venta local
 *      - Se marcan los asientos como VENDIDO en la BD local
 */
@Service
@Transactional
public class VentaSyncService {
    private static final Logger log = LoggerFactory.getLogger(VentaSyncService.class);
    private final EventoRepository eventoRepository;
    private final AsientoRepository asientoRepository;
    private final VentaRepository ventaRepository;
    private final AsientoEstadoService asientoEstadoService;
    private final ProxyService proxyService;

    public VentaSyncService(
        EventoRepository eventoRepository,
        AsientoRepository asientoRepository,
        VentaRepository ventaRepository,
        AsientoEstadoService asientoEstadoService,
        ProxyService proxyService
    ) {
        this.eventoRepository = eventoRepository;
        this.asientoRepository = asientoRepository;
        this.ventaRepository = ventaRepository;
        this.asientoEstadoService = asientoEstadoService;
        this.proxyService = proxyService;
    }

    /**
     * Procesa una venta solicitada por el frontend.
     *
     * IMPORTANTE:
     * - request.getEventoId() contiene el idLocal del evento (resuelto previamente por el controller).
     * - El externalId se obtiene desde la entidad Evento para comunicarse con la cátedra.
     *
     * Devuelve siempre el Payload 7 (ProxyVentaResponseDTO):
     * - resultado = true  → venta confirmada por la cátedra
     * - resultado = false → venta rechazada (bloqueos, reglas, etc.)
     */
    public ProxyVentaResponseDTO procesarVenta(VentaRequestFrontendDTO request) {

        if (request == null) throw new IllegalArgumentException("Body requerido.");
        if (request.getEventoId() == null) throw new IllegalArgumentException("eventoIdLocal requerido.");
        if (request.getAsientos() == null || request.getAsientos().isEmpty()) {
            throw new IllegalArgumentException("La venta debe incluir al menos un asiento.");
        }

        // eventoIdLocal (NO externalId)
        Long eventoIdLocal = request.getEventoId(); // 1051

        log.info(
            "💸 [Sync-Venta] Iniciando venta eventoIdLocal={} asientos={}",
            eventoIdLocal,
            request.getAsientos().size()
        );

        // 1) Validar evento local
        Evento evento = eventoRepository.findById(eventoIdLocal)
            .orElseThrow(() -> new IllegalArgumentException(
                "Evento no encontrado idLocal=" + eventoIdLocal
            ));

        if (Boolean.FALSE.equals(evento.getActivo())) {
            log.warn("⛔ [Sync-Venta] Venta sobre evento inactivo idLocal={}", eventoIdLocal);
            throw new IllegalStateException("No se pueden generar ventas sobre un evento inactivo.");
        }

        if (evento.getExternalId() == null) {
            log.warn(
                "⛔ [Sync-Venta] Evento idLocal={} sin externalId. No se puede vender contra cátedra.",
                eventoIdLocal
            );
            throw new IllegalStateException(
                "El evento no tiene externalId (id cátedra), no se puede vender."
            );
        }

        // 2) Obtener estados en tiempo real (Redis vía proxy)
        List<AsientoEstadoDTO> estados =
            asientoEstadoService.obtenerEstadoActualDeAsientos(eventoIdLocal);

        Map<String, AsientoEstadoDTO> mapaEstado = new HashMap<>();
        for (AsientoEstadoDTO dto : estados) {
            if (dto != null && dto.getFila() != null && dto.getColumna() != null) {
                mapaEstado.put(dto.getFila() + "-" + dto.getColumna(), dto);
            }
        }

        // 3) Validar asientos solicitados
        List<Asiento> asientosPersistidos = new ArrayList<>();

        for (VentaAsientoFrontendDTO asientoReq : request.getAsientos()) {

            if (asientoReq == null || asientoReq.getFila() == null || asientoReq.getColumna() == null) {
                throw new IllegalArgumentException("Cada asiento debe incluir fila y columna.");
            }
            if (asientoReq.getFila() < 1 || asientoReq.getColumna() < 1) {
                throw new IllegalArgumentException("Fila y columna deben ser >= 1.");
            }
            if (asientoReq.getPersona() == null || asientoReq.getPersona().isBlank()) {
                throw new IllegalArgumentException("Cada asiento debe incluir persona (payload 7).");
            }

            String key = asientoReq.getFila() + "-" + asientoReq.getColumna();
            AsientoEstadoDTO estadoDto = mapaEstado.get(key);

            if (estadoDto == null) {
                throw new IllegalStateException(
                    "Asiento (" + asientoReq.getFila() + "," + asientoReq.getColumna() + ") no válido para el evento."
                );
            }

            String estado = estadoDto.getEstado();

            if ("VENDIDO".equalsIgnoreCase(estado)) {
                throw new IllegalStateException(
                    "Asiento (" + asientoReq.getFila() + "," + asientoReq.getColumna() + ") ya está vendido."
                );
            }

            if (!"BLOQUEADO_VIGENTE".equalsIgnoreCase(estado)) {
                throw new IllegalStateException(
                    "Asiento (" + asientoReq.getFila() + "," + asientoReq.getColumna() +
                        ") no está bloqueado vigente. Estado actual: " + estado
                );
            }

            Asiento asiento = asientoRepository
                .findByEventoIdAndFilaAndColumna(eventoIdLocal, asientoReq.getFila(), asientoReq.getColumna())
                .orElseThrow(() -> new IllegalStateException(
                    "Asiento persistido no encontrado (eventoIdLocal=" + eventoIdLocal + ")"
                ));

            asientosPersistidos.add(asiento);
        }

        // 4) Calcular total
        int cantidadAsientos = asientosPersistidos.size();
        BigDecimal total = evento.getPrecioEntrada()
            .multiply(BigDecimal.valueOf(cantidadAsientos));

        // 5) Construir request para la cátedra (externalId)
        ProxyVentaRequestDTO requestProxy = new ProxyVentaRequestDTO();
        requestProxy.setEventoId(evento.getExternalId());
        requestProxy.setFecha(Instant.now());
        requestProxy.setPrecioVenta(total);
        requestProxy.setAsientos(request.getAsientos());

        log.info(
            "💸 [Sync-Venta] Enviando venta al proxy eventoIdLocal={} externalId={} total={}",
            eventoIdLocal,
            evento.getExternalId(),
            total
        );

        // 6) Confirmar venta con la cátedra
        ProxyVentaResponseDTO resp =
            proxyService.crearVentaEnProxy(evento.getExternalId(), requestProxy);

        if (resp == null) {
            throw new IllegalStateException(
                "No se pudo confirmar la venta con la cátedra (respuesta nula)."
            );
        }

        if (Boolean.FALSE.equals(resp.getResultado())) {
            log.warn("⛔ [Sync-Venta] Venta rechazada por cátedra: {}", resp.getDescripcion());
            return resp;
        }

        // 7) Persistir venta local
        Venta venta = new Venta();
        venta.setExternalId(resp.getVentaId());
        venta.setEstado(VentaEstado.CONFIRMADA);
        venta.setFechaVenta(
            resp.getFechaVenta() != null
                ? resp.getFechaVenta().atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now()
        );
        venta.setDescripcion(resp.getDescripcion());
        venta.setPrecioVenta(resp.getPrecioVenta());
        venta.setCantidadAsientos(cantidadAsientos);
        venta.setEvento(evento);
        venta.getAsientos().addAll(asientosPersistidos);

        ventaRepository.save(venta);

        for (Asiento a : asientosPersistidos) {
            a.setEstado(AsientoEstado.VENDIDO);
        }
        asientoRepository.saveAll(asientosPersistidos);

        log.info(
            "💾 [Sync-Venta] Venta persistida idLocal={} externalId={} asientos={}",
            venta.getId(),
            venta.getExternalId(),
            cantidadAsientos
        );

        return resp;
    }
}
