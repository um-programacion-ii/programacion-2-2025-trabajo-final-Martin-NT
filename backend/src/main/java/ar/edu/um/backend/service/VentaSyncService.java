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
 * Servicio de negocio para realizar ventas sincronizadas con la cátedra (vía proxy).
 *
 * Reglas del enunciado (Payload 7):
 * - La venta falla si se intenta vender un asiento "Libre" (no reservado/bloqueado)
 *   o "Ocupado" (ya vendido).
 * - La venta OK deja los asientos en estado "Vendido" (en la cátedra).
 *
 * Estrategia local:
 * 1) Validar evento activo y con externalId.
 * 2) Validar que todos los asientos estén BLOQUEADO_VIGENTE (según AsientoEstadoService).
 * 3) Construir ProxyVentaRequestDTO (eventoId externo + fecha + precioVenta + asientos con persona).
 * 4) Enviar al proxy y recibir ProxyVentaResponseDTO.
 * 5) Si resultado=true: persistir Venta + marcar asientos como VENDIDO en DB local.
 * 6) Si resultado=false: NO persistir venta y devolver error de negocio.
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
     * @param request incluye eventoIdLocal y asientos (fila/columna/persona).
     * @return Venta persistida localmente si la cátedra confirmó la venta.
     */
    public Venta procesarVenta(VentaRequestFrontendDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Body requerido.");
        }
        if (request.getEventoIdLocal() == null) {
            throw new IllegalArgumentException("eventoIdLocal requerido.");
        }
        if (request.getAsientos() == null || request.getAsientos().isEmpty()) {
            throw new IllegalArgumentException("La venta debe incluir al menos un asiento.");
        }

        Long eventoIdLocal = request.getEventoIdLocal();
        log.info("💸 [Sync-Venta] Iniciando venta para eventoIdLocal={} asientos={}", eventoIdLocal, request.getAsientos().size());

        // 1) Validar evento
        Evento evento = eventoRepository
            .findById(eventoIdLocal)
            .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado idLocal=" + eventoIdLocal));

        if (Boolean.FALSE.equals(evento.getActivo())) {
            log.warn("⛔ [Sync-Venta] Venta sobre evento inactivo idLocal={}", eventoIdLocal);
            throw new IllegalStateException("No se pueden generar ventas sobre un evento inactivo.");
        }

        if (evento.getExternalId() == null) {
            log.warn("⛔ [Sync-Venta] Evento idLocal={} sin externalId. No se puede vender contra cátedra.", eventoIdLocal);
            throw new IllegalStateException("El evento no tiene externalId (id cátedra), no se puede vender.");
        }

        // 2) Obtener estados en tiempo real (DB + Redis)
        List<AsientoEstadoDTO> estados = asientoEstadoService.obtenerEstadoActualDeAsientos(eventoIdLocal);

        Map<String, AsientoEstadoDTO> mapaEstado = new HashMap<>();
        for (AsientoEstadoDTO dto : estados) {
            if (dto != null && dto.getFila() != null && dto.getColumna() != null) {
                mapaEstado.put(dto.getFila() + "-" + dto.getColumna(), dto);
            }
        }

        // 3) Validar asientos y cargar entidades Asiento locales
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
                log.warn("⛔ [Sync-Venta] Asiento ({},{}) no existe en mapa estado eventoIdLocal={}", asientoReq.getFila(), asientoReq.getColumna(), eventoIdLocal);
                throw new IllegalStateException("Asiento (" + asientoReq.getFila() + "," + asientoReq.getColumna() + ") no es válido para este evento.");
            }

            String estado = estadoDto.getEstado(); // LIBRE / BLOQUEADO_VIGENTE / BLOQUEADO_EXPIRADO / VENDIDO

            if ("VENDIDO".equalsIgnoreCase(estado)) {
                log.warn("⛔ [Sync-Venta] Asiento ({},{}) ya vendido. Rechazando.", asientoReq.getFila(), asientoReq.getColumna());
                throw new IllegalStateException("Asiento (" + asientoReq.getFila() + "," + asientoReq.getColumna() + ") ya está vendido.");
            }

            if (!"BLOQUEADO_VIGENTE".equalsIgnoreCase(estado)) {
                log.warn("⛔ [Sync-Venta] Asiento ({},{}) no está BLOQUEADO_VIGENTE (estado={}). Rechazando.", asientoReq.getFila(), asientoReq.getColumna(), estado);
                throw new IllegalStateException("Asiento (" + asientoReq.getFila() + "," + asientoReq.getColumna() + ") no está bloqueado vigente. Estado actual: " + estado);
            }

            Asiento asiento = asientoRepository
                .findByEventoIdAndFilaAndColumna(eventoIdLocal, asientoReq.getFila(), asientoReq.getColumna())
                .orElseThrow(() -> new IllegalStateException("Asiento persistido no encontrado eventoIdLocal=" + eventoIdLocal + " fila=" + asientoReq.getFila() + " col=" + asientoReq.getColumna()));

            asientosPersistidos.add(asiento);
        }

        int cantidadAsientos = request.getAsientos().size();

        // 4) Calcular precio total (no confiar en request.getPrecioVenta())
        BigDecimal precioEntrada = evento.getPrecioEntrada();
        BigDecimal total = (precioEntrada != null)
            ? precioEntrada.multiply(BigDecimal.valueOf(cantidadAsientos))
            : BigDecimal.ZERO;

        // 5) Construir request para proxy/cátedra (Payload 7 entrada)
        ProxyVentaRequestDTO requestProxy = new ProxyVentaRequestDTO();
        requestProxy.setEventoId(evento.getExternalId());
        requestProxy.setFecha(Instant.now());
        requestProxy.setPrecioVenta(total);
        requestProxy.setAsientos(request.getAsientos()); // coincide con payload (fila/columna/persona)

        log.info("💸 [Sync-Venta] Enviando venta al proxy: eventoIdLocal={} externalId={} asientos={} total={}",
            eventoIdLocal, evento.getExternalId(), cantidadAsientos, total
        );

        // 6) Llamar a proxy
        ProxyVentaResponseDTO resp = proxyService.crearVentaEnProxy(evento.getExternalId(), requestProxy);

        if (resp == null) {
            log.error("❌ [Sync-Venta] Respuesta nula desde proxy (integración).");
            throw new IllegalStateException("No se pudo confirmar la venta con la cátedra (respuesta nula).");
        }

        // 7) Interpretar respuesta (Payload 7 salida)
        if (Boolean.FALSE.equals(resp.getResultado())) {
            String motivo = (resp.getDescripcion() != null) ? resp.getDescripcion() : "Venta rechazada por la cátedra.";
            log.warn("⛔ [Sync-Venta] Venta rechazada por cátedra: {}", motivo);
            throw new IllegalStateException(motivo);
        }

        log.info("✅ [Sync-Venta] Venta confirmada por cátedra: ventaId={} descripcion={}", resp.getVentaId(), resp.getDescripcion());

        // 8) Persistir venta local + marcar asientos vendidos
        Venta venta = new Venta();
        venta.setExternalId(resp.getVentaId()); // puede venir null si cátedra lo permite, pero en payload OK suele venir
        venta.setEstado(VentaEstado.CONFIRMADA);

        if (resp.getFechaVenta() != null) {
            venta.setFechaVenta(resp.getFechaVenta().atZone(ZoneId.systemDefault()).toLocalDate());
        } else {
            venta.setFechaVenta(LocalDate.now());
        }

        venta.setDescripcion(resp.getDescripcion() != null ? resp.getDescripcion() : "Venta realizada con éxito");
        venta.setPrecioVenta(resp.getPrecioVenta() != null ? resp.getPrecioVenta() : total);
        venta.setCantidadAsientos(cantidadAsientos);
        venta.setEvento(evento);
        venta.getAsientos().addAll(asientosPersistidos);

        Venta guardada = ventaRepository.save(venta);

        for (Asiento asiento : asientosPersistidos) {
            asiento.setEstado(AsientoEstado.VENDIDO);
        }
        asientoRepository.saveAll(asientosPersistidos);

        log.info("💾 [Sync-Venta] Venta guardada idLocal={} externalId={} asientos={} -> asientos marcados VENDIDO",
            guardada.getId(), guardada.getExternalId(), cantidadAsientos
        );

        return guardada;
    }
}
