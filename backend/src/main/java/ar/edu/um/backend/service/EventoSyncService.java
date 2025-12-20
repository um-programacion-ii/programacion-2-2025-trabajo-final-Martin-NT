package ar.edu.um.backend.service;
import ar.edu.um.backend.domain.Evento;
import ar.edu.um.backend.repository.EventoRepository;
import ar.edu.um.backend.service.dto.ProxyEventoDetalleDTO;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
/**
 * Servicio encargado de sincronizar la base de datos local de eventos
 * con la información real proveniente de la cátedra, accesible a través del proxy.
 *
 * Flujo:
 *  1) Obtiene eventos tipados desde ProxyService (Payload 4 / eventos completos).
 *  2) Crea o actualiza eventos locales según externalId.
 *  3) Sincroniza asientos por evento.
 *  4) Marca como inactivos los eventos que ya no existen en la cátedra.
 */
@Service
@Transactional
public class EventoSyncService {

    private static final Logger log = LoggerFactory.getLogger(EventoSyncService.class);

    private final ProxyService proxyService;
    private final EventoRepository eventoRepository;
    private final AsientoSyncService asientoSyncService;

    public EventoSyncService(
        ProxyService proxyService,
        EventoRepository eventoRepository,
        AsientoSyncService asientoSyncService
    ) {
        this.proxyService = proxyService;
        this.eventoRepository = eventoRepository;
        this.asientoSyncService = asientoSyncService;
    }

    public void sincronizarEventosDesdeProxy() {

        log.info("🔄 [Sync-Eventos] Iniciando sincronización de eventos contra proxy...");

        // 1) Obtener eventos tipados desde el proxy (Payload 4)
        List<ProxyEventoDetalleDTO> remotos = proxyService.listarEventosCompletos();

        if (remotos == null || remotos.isEmpty()) {
            log.warn("⚠️ [Sync-Eventos] No se recibieron eventos desde el proxy.");
            return;
        }

        log.info("📥 [Sync-Eventos] Eventos recibidos desde proxy: {} evento(s).", remotos.size());

        Set<Long> externalIdsVigentes = new HashSet<>();

        // Usamos UTC porque la cátedra manda timestamps con "Z"
        ZoneId zone = ZoneOffset.UTC;

        // 2) Procesar evento por evento
        for (ProxyEventoDetalleDTO remoto : remotos) {

            if (remoto.getId() == null) {
                log.warn("⚠️ [Sync-Eventos] Evento remoto ignorado (sin ID). Título={}", remoto.getTitulo());
                continue;
            }

            externalIdsVigentes.add(remoto.getId());

            Evento local = eventoRepository
                .findByExternalId(remoto.getId())
                .orElseGet(Evento::new);

            if (local.getId() == null) {
                log.info("🆕 [Sync-Eventos] Creando evento nuevo (externalId={}) → {}", remoto.getId(), remoto.getTitulo());
                local.setExternalId(remoto.getId());
            } else {
                log.info(
                    "♻️ [Sync-Eventos] Actualizando evento (idLocal={}, externalId={}) → {}",
                    local.getId(),
                    remoto.getId(),
                    remoto.getTitulo()
                );
            }

            // Siempre activo si viene del proxy
            local.setActivo(true);

            // ---------------- MAPEOS ----------------

            // Fecha/Hora (Instant -> LocalDate + LocalTime)
            Instant fechaInst = remoto.getFecha();
            if (fechaInst == null) {
                // fallback si viniera mal (no debería)
                LocalDate hoy = LocalDate.now(zone);
                local.setFecha(hoy);
                local.setHora(LocalTime.MIDNIGHT);
                log.warn("⚠️ Evento {} sin fecha (Instant). Se asigna fecha actual y 00:00 UTC.", remoto.getId());
            } else {
                LocalDateTime ldt = LocalDateTime.ofInstant(fechaInst, zone);
                local.setFecha(ldt.toLocalDate());
                local.setHora(ldt.toLocalTime());
            }

            // Asientos
            Integer filas = remoto.getFilaAsientos();
            Integer columnas = remoto.getColumnaAsientos();

            if (filas == null || columnas == null || filas <= 0 || columnas <= 0) {
                log.error(
                    "❌ Evento {} con configuración inválida de asientos (filas={}, columnas={}).",
                    remoto.getId(), filas, columnas
                );
                continue;
            }

            local.setFilaAsientos(filas);
            local.setColumnaAsientos(columnas);
            local.setCantidadAsientosTotales(filas * columnas);

            // Precio
            BigDecimal precio = remoto.getPrecioEntrada();
            if (precio == null) {
                precio = BigDecimal.ZERO;
                log.warn("⚠️ Evento {} sin precioEntrada. Se asigna 0.", remoto.getId());
            } else if (precio.compareTo(BigDecimal.ZERO) < 0) {
                log.error("❌ Evento {} con precioEntrada negativo ({}). Ignorado.", remoto.getId(), precio);
                continue;
            }
            local.setPrecioEntrada(precio);

            // Datos generales (lo que tu entidad local realmente usa)
            local.setTitulo(remoto.getTitulo());
            local.setDescripcion(remoto.getDescripcion());

            // OJO: organizador/presentadores no vienen en payloads 3/4/5.
            // Si tu entidad los requiere en otro lado, lo tratamos aparte.
            // local.setOrganizador(null);
            // local.setPresentadores(null);

            Evento guardado = eventoRepository.save(local);

            log.info(
                "💾 [DB] Evento guardado → idLocal={}, externalId={}, titulo={}",
                guardado.getId(),
                remoto.getId(),
                guardado.getTitulo()
            );

            // 3) Sincronizar asientos del evento
            asientoSyncService.sincronizarAsientosDeEvento(guardado, remoto.getId());
        }

        // 4) Marcar eventos inactivos
        List<Evento> eventosLocales = eventoRepository.findByExternalIdIsNotNull();

        for (Evento evento : eventosLocales) {
            if (!externalIdsVigentes.contains(evento.getExternalId())
                && Boolean.TRUE.equals(evento.getActivo())) {

                evento.setActivo(false);
                eventoRepository.save(evento);

                log.info(
                    "🗑️ [Sync-Eventos] Evento externalId={} marcado como inactivo (idLocal={})",
                    evento.getExternalId(),
                    evento.getId()
                );
            }
        }

        log.info("✅ [Sync-Eventos] Sincronización de eventos finalizada correctamente.");
    }
}
