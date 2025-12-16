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
import ar.edu.um.backend.service.dto.AsientoVentaDTO;
import ar.edu.um.backend.service.dto.ProxyVentaAsientoDTO;
import ar.edu.um.backend.service.dto.ProxyVentaDTO;
import ar.edu.um.backend.service.dto.VentaRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de sincronización de ventas con la cátedra.
 *
 * Flujo principal:
 *  - Validar asientos bloqueados (usando AsientoEstadoService: DB + Redis).
 *  - Construir el request para el proxy (eventoId + asientos + precioTotal).
 *  - Enviar la venta al proxy con ProxyService.crearVentaEnProxy().
 *  - Interpretar la respuesta de la cátedra:
 *      * Soportar 200 OK sin body (caso actual).
 *      * Soportar JSON de respuesta (estructura ProxyVentaDTO) si en un futuro lo devuelven.
 *  - Persistir la venta local sólo si la cátedra confirma (resultado=true).
 *  - Marcar los asientos como VENDIDO en la base local.
 *  - Procesar notificaciones posteriores (Kafka → proxy → backend).
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
    private final ObjectMapper objectMapper;

    public VentaSyncService(
        EventoRepository eventoRepository,
        AsientoRepository asientoRepository,
        VentaRepository ventaRepository,
        AsientoEstadoService asientoEstadoService,
        ProxyService proxyService,
        ObjectMapper objectMapper
    ) {
        this.eventoRepository = eventoRepository;
        this.asientoRepository = asientoRepository;
        this.ventaRepository = ventaRepository;
        this.asientoEstadoService = asientoEstadoService;
        this.proxyService = proxyService;
        this.objectMapper = objectMapper;
    }

    /**
     * Paso principal: procesar una venta solicitada por el usuario.
     *
     * 1) Validar que el evento exista y esté activo.
     * 2) Validar que todos los asientos estén bloqueados vigentes (Redis) y no vendidos.
     * 3) Construir request para el proxy (eventoId externo + asientos + precioTotal).
     * 4) Llamar al proxy y procesar la respuesta (200 sin body o JSON).
     * 5) Si resultado=true → guardar Venta local + marcar asientos como VENDIDO.
     */
    public Venta procesarVenta(VentaRequestDTO request) {
        Long eventoIdLocal = request.getEventoIdLocal();

        log.info("💸 [Sync-Venta] Iniciando procesamiento de venta para eventoIdLocal={} ...", eventoIdLocal);

        // 1) Validar que el evento exista y esté activo
        Evento evento = eventoRepository
            .findById(eventoIdLocal)
            .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado para idLocal=" + eventoIdLocal));

        if (Boolean.FALSE.equals(evento.getActivo())) {
            log.warn("⛔ [Sync-Venta] Intento de venta sobre evento inactivo idLocal={}", eventoIdLocal);
            throw new IllegalStateException("No se pueden generar ventas sobre un evento inactivo.");
        }

        if (request.getAsientos() == null || request.getAsientos().isEmpty()) {
            log.warn("⛔ [Sync-Venta] Request de venta sin asientos para eventoIdLocal={}", eventoIdLocal);
            throw new IllegalArgumentException("La venta debe incluir al menos un asiento.");
        }

        // 2) Obtener estado en tiempo real de los asientos (DB + Redis)
        List<AsientoEstadoDTO> estados = asientoEstadoService.obtenerEstadoActualDeAsientos(eventoIdLocal);

        Map<String, AsientoEstadoDTO> mapaEstado = new HashMap<>();
        for (AsientoEstadoDTO dto : estados) {
            String key = dto.getFila() + "-" + dto.getColumna();
            mapaEstado.put(key, dto);
        }

        // 3) Validar que todos los asientos solicitados estén BLOQUEADO_VIGENTE y no vendidos
        List<Asiento> asientosPersistidos = new ArrayList<>();

        for (AsientoVentaDTO asientoReq : request.getAsientos()) {
            Integer fila = asientoReq.getFila();
            Integer columna = asientoReq.getColumna();
            String key = fila + "-" + columna;

            AsientoEstadoDTO estadoDto = mapaEstado.get(key);

            if (estadoDto == null) {
                log.warn(
                    "⛔ [Sync-Venta] Asiento ({},{}) no existe en mapa de estado para eventoIdLocal={}",
                    fila,
                    columna,
                    eventoIdLocal
                );
                throw new IllegalStateException("Asiento (" + fila + "," + columna + ") no es válido para este evento.");
            }

            String estado = estadoDto.getEstado(); // LIBRE / BLOQUEADO_VIGENTE / BLOQUEADO_EXPIRADO / VENDIDO

            if ("VENDIDO".equals(estado)) {
                log.warn("⛔ [Sync-Venta] Asiento ({},{}) ya está vendido. Venta rechazada.", fila, columna);
                throw new IllegalStateException("Asiento (" + fila + "," + columna + ") ya está vendido.");
            }

            if (!"BLOQUEADO_VIGENTE".equals(estado)) {
                log.warn(
                    "⛔ [Sync-Venta] Bloqueo vencido o inexistente para asiento ({},{}) (estado={}). Venta rechazada.",
                    fila,
                    columna,
                    estado
                );
                throw new IllegalStateException(
                    "Asiento (" + fila + "," + columna + ") no está bloqueado vigente. Estado actual: " + estado
                );
            }

            log.info("🔒 [Sync-Venta] Asiento ({},{}) bloqueado vigente → válido para venta.", fila, columna);

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

        int cantidadAsientos = request.getAsientos().size();

        // Precio total = precioEntrada * cantidadAsientos
        BigDecimal precioEntrada = evento.getPrecioEntrada();
        BigDecimal total =
            precioEntrada != null ? precioEntrada.multiply(BigDecimal.valueOf(cantidadAsientos)) : BigDecimal.ZERO;

        // 4) Construir request para el proxy/cátedra.
        //    Usamos ProxyVentaDTO como DTO de integración también para el request.
        ProxyVentaDTO requestProxy = new ProxyVentaDTO();
        requestProxy.setEventoId(evento.getExternalId());
        requestProxy.setPrecioVenta(total);

        // Convertimos AsientoVentaDTO -> ProxyVentaAsientoDTO (solo fila/columna, persona/estado nulos)
        List<ProxyVentaAsientoDTO> asientosProxy = new ArrayList<>();
        for (AsientoVentaDTO a : request.getAsientos()) {
            ProxyVentaAsientoDTO pa = new ProxyVentaAsientoDTO();
            pa.setFila(a.getFila());
            pa.setColumna(a.getColumna());
            pa.setPersona(null);
            pa.setEstado(null);
            asientosProxy.add(pa);
        }
        requestProxy.setAsientos(asientosProxy);

        log.info(
            "💸 [Sync-Venta] Enviando venta a proxy: eventoLocalId={}, externalId={}, asientos={}, total={}",
            eventoIdLocal,
            evento.getExternalId(),
            cantidadAsientos,
            total
        );

        // 5) Llamar al proxy para crear la venta real en la cátedra
        String respuestaProxyJson = proxyService.crearVentaEnProxy(evento.getExternalId(), requestProxy);

        if (respuestaProxyJson == null) {
            log.error("❌ [Sync-Venta] Respuesta nula desde proxy al crear venta. Venta NO será persistida.");
            throw new IllegalStateException("No se pudo confirmar la venta con la cátedra.");
        }

        // 👉 Soportamos dos casos:
        //  - String vacío ("") → la cátedra respondió 200 OK sin body.
        //  - JSON con estructura ProxyVentaDTO (posible implementación futura del P7).
        ProxyVentaDTO respuestaProxy;

        if (respuestaProxyJson.isBlank()) {
            // Caso actual: 200 OK sin body → construimos una respuesta sintética exitosa
            respuestaProxy = construirRespuestaVentaOkLocal(evento, cantidadAsientos, total);
            log.info("💸 [Sync-Venta] Venta confirmada por cátedra (200 OK sin body). Se usará respuesta sintética.");
        } else {
            // Caso futuro: la cátedra efectivamente devuelve el JSON del P7
            respuestaProxy = parsearRespuestaVenta(respuestaProxyJson);

            if (respuestaProxy == null) {
                log.error("❌ [Sync-Venta] No se pudo parsear la respuesta de venta de la cátedra.");
                throw new IllegalStateException("Respuesta de la cátedra inválida al registrar la venta.");
            }

            if (Boolean.FALSE.equals(respuestaProxy.getResultado())) {
                log.warn(
                    "⛔ [Sync-Venta] La cátedra rechazó la venta. descripcion='{}'. JSON={}",
                    respuestaProxy.getDescripcion(),
                    respuestaProxyJson
                );
                throw new IllegalStateException(
                    "La cátedra no confirmó la venta. Motivo: " + respuestaProxy.getDescripcion()
                );
            }
        }

        log.info(
            "💸 [Sync-Venta] Venta confirmada por cátedra. ventaId={}, descripcion={}",
            respuestaProxy.getVentaId(),
            respuestaProxy.getDescripcion()
        );

        // 6) Construir y guardar la Venta local
        Venta venta = new Venta();

        // Guardamos el ID real de la cátedra para futuras sincronizaciones (P8 / notificaciones).
        // En el caso actual (200 sin body) será null.
        venta.setExternalId(respuestaProxy.getVentaId());

        if (respuestaProxy.getFechaVenta() != null) {
            LocalDate fechaLocal = respuestaProxy
                .getFechaVenta()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
            venta.setFechaVenta(fechaLocal);
        } else {
            venta.setFechaVenta(LocalDate.now());
        }

        venta.setEstado(VentaEstado.CONFIRMADA);

        String desc = respuestaProxy.getDescripcion() != null
            ? respuestaProxy.getDescripcion()
            : "Venta confirmada por cátedra.";
        venta.setDescripcion(desc);

        BigDecimal totalFinal = respuestaProxy.getPrecioVenta() != null ? respuestaProxy.getPrecioVenta() : total;
        venta.setPrecioVenta(totalFinal);

        Integer cantFinal = respuestaProxy.getCantidadAsientos() != null
            ? respuestaProxy.getCantidadAsientos()
            : cantidadAsientos;
        venta.setCantidadAsientos(cantFinal);

        venta.setEvento(evento);
        venta.getAsientos().addAll(asientosPersistidos);

        Venta guardada = ventaRepository.save(venta);

        // 7) Marcar asientos como VENDIDO en la base local
        for (Asiento asiento : asientosPersistidos) {
            asiento.setEstado(AsientoEstado.VENDIDO);
        }
        asientoRepository.saveAll(asientosPersistidos);

        log.info(
            "💾 [Sync-Venta] Venta idLocal={} (externalId={}) guardada correctamente con {} asiento(s). Asientos marcados como VENDIDO.",
            guardada.getId(),
            guardada.getExternalId(),
            cantFinal
        );

        return guardada;
    }

    /**
     * Construye una respuesta de venta exitosa "sintética" cuando la cátedra
     * responde 200 OK pero sin body (content-length: 0).
     */
    private ProxyVentaDTO construirRespuestaVentaOkLocal(Evento evento, int cantidadAsientos, BigDecimal total) {
        ProxyVentaDTO dto = new ProxyVentaDTO();

        dto.setEventoId(evento.getExternalId());
        dto.setVentaId(null); // la cátedra no envía ventaId en el body actual
        dto.setFechaVenta(Instant.now());
        dto.setResultado(true);
        dto.setDescripcion("Venta realizada con éxito (200 OK sin body desde la cátedra)");
        dto.setPrecioVenta(total);
        dto.setCantidadAsientos(cantidadAsientos);

        return dto;
    }

    /**
     * Parsea la respuesta JSON de venta del proxy/cátedra al DTO ProxyVentaDTO.
     */
    private ProxyVentaDTO parsearRespuestaVenta(String json) {
        try {
            return objectMapper.readValue(json, ProxyVentaDTO.class);
        } catch (Exception e) {
            log.error("💥 [Sync-Venta] Error parseando JSON de respuesta de venta: {}", json, e);
            return null;
        }
    }

    /**
     * Procesa una notificación de venta enviada por el proxy
     * (típicamente originada en Kafka en la cátedra).
     *
     * Espera un JSON con la misma estructura de ProxyVentaDTO.
     * Usa ventaId como externalId para ubicar la venta local y actualizar su estado.
     */
    public void procesarNotificacionVenta(String mensajeKafkaCrudo) {
        log.info("📨 [Sync-Venta] Notificación de venta recibida desde proxy: {}", mensajeKafkaCrudo);

        ProxyVentaDTO dto = parsearRespuestaVenta(mensajeKafkaCrudo);
        if (dto == null || dto.getVentaId() == null) {
            log.warn("⚠️ [Sync-Venta] Notificación de venta inválida o sin ventaId. No se actualiza nada.");
            return;
        }

        Venta venta;
        try {
            venta = ventaRepository
                .findByExternalId(dto.getVentaId())
                .orElseThrow(() -> new IllegalStateException("No se encontró venta local con externalId=" + dto.getVentaId()));
        } catch (IllegalStateException e) {
            log.warn("⚠️ [Sync-Venta] {}. No se actualiza nada.", e.getMessage());
            return;
        }

        // Actualizar estado según resultado
        if (Boolean.FALSE.equals(dto.getResultado())) {
            venta.setEstado(VentaEstado.RECHAZADA);
        } else {
            venta.setEstado(VentaEstado.CONFIRMADA);
        }

        if (dto.getDescripcion() != null) {
            venta.setDescripcion(dto.getDescripcion());
        }
        if (dto.getPrecioVenta() != null) {
            venta.setPrecioVenta(dto.getPrecioVenta());
        }
        if (dto.getCantidadAsientos() != null) {
            venta.setCantidadAsientos(dto.getCantidadAsientos());
        }
        if (dto.getFechaVenta() != null) {
            venta.setFechaVenta(dto.getFechaVenta().atZone(ZoneId.systemDefault()).toLocalDate());
        }

        ventaRepository.save(venta);

        log.info(
            "✅ [Sync-Venta] Venta local id={} (externalId={}) actualizada desde notificación. Nuevo estado={}.",
            venta.getId(),
            venta.getExternalId(),
            venta.getEstado()
        );
    }
}
