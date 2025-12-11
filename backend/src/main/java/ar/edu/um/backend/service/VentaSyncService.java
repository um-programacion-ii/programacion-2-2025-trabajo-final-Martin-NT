package ar.edu.um.backend.service;

import ar.edu.um.backend.domain.Asiento;
import ar.edu.um.backend.domain.Evento;
import ar.edu.um.backend.domain.Venta;
import ar.edu.um.backend.domain.enumeration.VentaEstado;
import ar.edu.um.backend.repository.AsientoRepository;
import ar.edu.um.backend.repository.EventoRepository;
import ar.edu.um.backend.repository.VentaRepository;
import ar.edu.um.backend.service.dto.AsientoEstadoDTO;
import ar.edu.um.backend.service.dto.AsientoVentaDTO;
import ar.edu.um.backend.service.dto.ProxyVentaDTO;
import ar.edu.um.backend.service.dto.VentaRequestDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de sincronización de ventas con la cátedra.
 *
 * Flujo principal:
 * - Validar asientos bloqueados (llamando internamente a AsientoEstadoService).
 * - Construir el objeto ProxyVentaDTO.
 * - Enviar la venta al proxy con ProxyService.crearVentaEnProxy().
 * - Interpretar la respuesta.
 * - Persistir la venta local sólo si la cátedra confirma.
 * - Manejar notificaciones posteriores (Kafka → Proxy → Backend).
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
     * Paso principal: procesar una venta solicitada por el usuario.
     * 1) Validar bloqueos.
     * 2) Construir DTO para proxy/cátedra.
     * 3) Llamar al proxy.
     * 4) Persistir venta si la cátedra confirma.
     */
    public Venta procesarVenta(VentaRequestDTO request) {
        Long eventoIdLocal = request.getEventoIdLocal();

        log.info("💸 [Venta] Iniciando procesamiento de venta para eventoIdLocal={} ...", eventoIdLocal);

        // 1) Validar que el evento exista y esté activo
        Evento evento = eventoRepository
            .findById(eventoIdLocal)
            .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado para idLocal=" + eventoIdLocal));

        if (Boolean.FALSE.equals(evento.getActivo())) {
            log.warn("⛔ [Venta] Intento de venta sobre evento inactivo idLocal={}", eventoIdLocal);
            throw new IllegalStateException("No se pueden generar ventas sobre un evento inactivo.");
        }

        if (request.getAsientos() == null || request.getAsientos().isEmpty()) {
            log.warn("⛔ [Venta] Request de venta sin asientos para eventoIdLocal={}", eventoIdLocal);
            throw new IllegalArgumentException("La venta debe incluir al menos un asiento.");
        }

        // 2) Obtener estado en tiempo real de los asientos (DB + Redis)
        List<AsientoEstadoDTO> estados = asientoEstadoService.obtenerEstadoActualDeAsientos(eventoIdLocal);

        // Pasar a mapa fila-columna → estado
        Map<String, AsientoEstadoDTO> mapaEstado = new HashMap<>();
        for (AsientoEstadoDTO dto : estados) {
            String key = dto.getFila() + "-" + dto.getColumna();
            mapaEstado.put(key, dto);
        }

        // 3) Validar que todos los asientos solicitados estén BLOQUEADO_VIGENTE
        List<Asiento> asientosPersistidos = new ArrayList<>();

        for (AsientoVentaDTO asientoReq : request.getAsientos()) {
            Integer fila = asientoReq.getFila();
            Integer columna = asientoReq.getColumna();
            String key = fila + "-" + columna;

            AsientoEstadoDTO estadoDto = mapaEstado.get(key);

            if (estadoDto == null) {
                log.warn(
                    "⛔ [Venta] Asiento ({},{}) no existe en mapa de estado para eventoIdLocal={}",
                    fila,
                    columna,
                    eventoIdLocal
                );
                throw new IllegalStateException("Asiento (" + fila + "," + columna + ") no es válido para este evento.");
            }

            String estado = estadoDto.getEstado(); // LIBRE / BLOQUEADO_VIGENTE / BLOQUEADO_EXPIRADO / VENDIDO

            if ("VENDIDO".equals(estado)) {
                log.warn("⛔ [Venta] Asiento ({},{}) ya está vendido. Venta rechazada.", fila, columna);
                throw new IllegalStateException("Asiento (" + fila + "," + columna + ") ya está vendido.");
            }

            if (!"BLOQUEADO_VIGENTE".equals(estado)) {
                // Cubre bloqueo inexistente / expirado / libre / cualquier otro.
                log.warn(
                    "⛔ [Venta] Bloqueo vencido o inexistente para asiento ({},{}) (estado={}). Venta rechazada.",
                    fila,
                    columna,
                    estado
                );
                throw new IllegalStateException(
                    "Asiento (" + fila + "," + columna + ") no está bloqueado vigente. Estado actual: " + estado
                );
            }

            log.info("🔒 [Venta] Asiento ({},{}) bloqueado vigente → válido para venta.", fila, columna);

            // Buscar asiento persistido en DB (para vincularlo a la Venta)
            Asiento asiento = asientoRepository
                .findByEventoIdAndFilaAndColumna(eventoIdLocal, fila, columna)
                .orElseThrow(() ->
                    new IllegalStateException(
                        "Asiento persistido no encontrado para eventoIdLocal=" +
                            eventoIdLocal +
                            " fila=" +
                            fila +
                            " columna=" +
                            columna
                    )
                );

            asientosPersistidos.add(asiento);
        }

        // 4) Construir ProxyVentaDTO para enviar al proxy/cátedra
        ProxyVentaDTO proxyVentaDTO = new ProxyVentaDTO();
        proxyVentaDTO.setEventoId(evento.getExternalId()); // id real en la cátedra
        proxyVentaDTO.setAsientos(new ArrayList<>(request.getAsientos()));

        int cantidadAsientos = request.getAsientos().size();

        // Precio total = precioEntrada * cantidadAsientos
        BigDecimal precioEntrada = evento.getPrecioEntrada();
        BigDecimal total =
            precioEntrada != null ? precioEntrada.multiply(BigDecimal.valueOf(cantidadAsientos)) : BigDecimal.ZERO;
        proxyVentaDTO.setPrecioTotal(total);

        log.info(
            "💸 [Venta] Enviando venta a proxy: eventoLocalId={}, externalId={}, asientos={}, total={}",
            eventoIdLocal,
            evento.getExternalId(),
            cantidadAsientos,
            total
        );

        // 5) Llamar al proxy para crear la venta real en la cátedra
        String respuestaProxy = proxyService.crearVentaEnProxy(evento.getExternalId(), proxyVentaDTO);

        if (respuestaProxy == null) {
            log.error("❌ [Venta] Respuesta nula desde proxy al crear venta. Venta NO será persistida.");
            throw new IllegalStateException("No se pudo confirmar la venta con la cátedra.");
        }

        // ⚠️ Provisorio: hasta conocer el JSON real de respuesta.
        boolean ventaConfirmadaPorCatedra =
            respuestaProxy.contains("CONFIRMADA") ||
                respuestaProxy.contains("success") ||
                respuestaProxy.contains("\"ok\"");

        if (!ventaConfirmadaPorCatedra) {
            log.warn("⛔ [Venta] La cátedra no confirmó la venta. Respuesta: {}", respuestaProxy);
            throw new IllegalStateException("La cátedra no confirmó la venta. Venta rechazada.");
        }

        log.info("💸 [Venta] Venta confirmada por cátedra. Persistiendo venta local...");

        // 6) Construir y guardar la Venta local
        Venta venta = new Venta();
        venta.setFechaVenta(LocalDate.now());
        // Ajustar al enum real: PENDIENTE / CONFIRMADA / RECHAZADA
        venta.setEstado(VentaEstado.CONFIRMADA);
        venta.setDescripcion("Venta confirmada por cátedra. Respuesta: " + respuestaProxy);
        venta.setPrecioVenta(total);
        venta.setCantidadAsientos(cantidadAsientos);
        venta.setEvento(evento);
        venta.getAsientos().addAll(asientosPersistidos);

        Venta guardada = ventaRepository.save(venta);

        log.info(
            "💾 [Venta] Venta id={} guardada correctamente con {} asiento(s).",
            guardada.getId(),
            cantidadAsientos
        );

        return guardada;
    }

    /**
     * Cuando el proxy envía una notificación de venta (Kafka → Proxy → Backend),
     * este método actualizará la venta local.
     * (Se implementará en un paso posterior cuando se defina el JSON real).
     */
    public void procesarNotificacionVenta(String mensajeKafkaCrudo) {
        log.info("📨 [Venta-Notify] Notificación de venta recibida desde proxy: {}", mensajeKafkaCrudo);

        // TODO:
        //  - Parsear el JSON de notificación.
        //  - Buscar la venta local por algún identificador (idVentaCatedra, idEvento, etc.).
        //  - Actualizar su estado a EXITOSA o FALLIDA según la notificación.
    }
}
