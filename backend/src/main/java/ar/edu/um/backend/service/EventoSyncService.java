package ar.edu.um.backend.service;

import ar.edu.um.backend.domain.Evento;
import ar.edu.um.backend.repository.EventoRepository;
import ar.edu.um.backend.service.dto.ProxyEventoDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servicio encargado de sincronizar la base de datos local de eventos
 * con la información real proveniente de la cátedra, accesible a través del proxy.
 *
 * Este servicio se ejecuta cuando:
 *   - el administrador lo solicita manualmente,
 *   - el proxy notifica cambios (Kafka → proxy → backend).
 *
 * El proceso consiste en:
 *    1. Obtener el JSON real de eventos desde el proxy.
 *    2. Convertir el JSON en {@link ProxyEventoDTO}.
 *    3. Por cada evento:
 *        - si no existe en la BD → se crea
 *        - si existe → se actualiza
 *    4. Aplicar valores por defecto cuando la cátedra no provee campos.
 *    5. Sincronizar asientos de cada evento usando {@link AsientoSyncService}.
 *    6. Marcar como inactivos los eventos locales que ya no vengan del proxy.
 *
 * Es el "orquestador" principal de la sincronización de eventos y asientos.
 */
@Service
@Transactional  // Garantiza atomicidad: si falla algo → rollback de cambios
public class EventoSyncService {

    private static final Logger log = LoggerFactory.getLogger(EventoSyncService.class);

    private final ProxyService proxyService;          // Cliente que consulta al proxy
    private final EventoRepository eventoRepository; // Acceso a la base local
    private final ObjectMapper objectMapper;          // Convierte JSON → objetos Java
    private final AsientoSyncService asientoSyncService;

    public EventoSyncService(
        ProxyService proxyService,
        EventoRepository eventoRepository,
        ObjectMapper objectMapper,
        AsientoSyncService asientoSyncService
    ) {
        this.proxyService = proxyService;
        this.eventoRepository = eventoRepository;
        this.objectMapper = objectMapper;
        this.asientoSyncService = asientoSyncService;
    }

    /**
     * Sincroniza los eventos locales con los datos provenientes del proxy.
     *
     * Flujo:
     *  - Llama a {@link ProxyService#listarEventosCompletos()} para obtener el JSON.
     *  - Parsea a un arreglo de {@link ProxyEventoDTO}.
     *  - Crea/actualiza eventos locales según su externalId.
     *  - Valida datos críticos (fecha, hora, filas/columnas de asientos, precioEntrada).
     *  - Llama a {@link AsientoSyncService} para sincronizar asientos evento por evento.
     *  - Marca como inactivos los eventos que ya no vienen en el listado remoto.
     */
    public void sincronizarEventosDesdeProxy() {

        log.info("🔄 [Sync-Eventos] Iniciando sincronización de eventos contra proxy...");

        // 1. Obtener JSON desde el proxy
        String json = proxyService.listarEventosCompletos();

        if (json == null) {
            log.warn("⚠️  [Sync-Eventos] No se pudo obtener la lista de eventos desde el proxy. Cancelando sincronización.");
            return;
        }

        try {
            // 2. Convertir JSON → lista de eventos remotos
            ProxyEventoDTO[] remotosArray = objectMapper.readValue(json, ProxyEventoDTO[].class);
            List<ProxyEventoDTO> remotos = Arrays.asList(remotosArray);

            log.info("📥 [Sync-Eventos] Eventos recibidos desde proxy: {} evento(s).", remotos.size());

            // Conjunto de IDs externos que siguen "vivos" en la cátedra
            Set<Long> externalIdsVigentes = new HashSet<>();

            // 3. Procesar evento por evento
            for (ProxyEventoDTO remoto : remotos) {

                // Validación mínima: todo evento debe tener un ID externo
                if (remoto.getId() == null) {
                    log.warn("⚠️  [Sync-Eventos] Evento remoto ignorado (sin ID). Título={}", remoto.getTitulo());
                    continue;
                }

                externalIdsVigentes.add(remoto.getId());

                // Buscar si ya existe un evento con ese externalId
                Optional<Evento> optLocal = eventoRepository.findByExternalId(remoto.getId());
                Evento local = optLocal.orElseGet(Evento::new);

                // Crear o actualizar
                if (local.getId() == null) {
                    log.info("🆕 [Sync-Eventos] Creando evento nuevo (externalId={}) → {}", remoto.getId(), remoto.getTitulo());
                    local.setExternalId(remoto.getId());
                } else {
                    log.info(
                        "♻️  [Sync-Eventos] Actualizando evento existente (id={}, externalId={}) → {}",
                        local.getId(),
                        remoto.getId(),
                        remoto.getTitulo()
                    );
                }

                // Siempre que viene del proxy, el evento debe quedar activo
                local.setActivo(true);

                // -------------- MAPEO DE CAMPOS -----------------

                // FECHA
                LocalDate fecha = remoto.getFecha();
                if (fecha == null) {
                    fecha = LocalDate.now(); // fallback si cátedra no envía fecha
                    log.warn(
                        "⚠️  [Sync-Eventos] El evento {} no tiene fecha en el proxy. Se asigna fecha actual: {}",
                        remoto.getId(),
                        fecha
                    );
                }
                local.setFecha(fecha);

                // HORA
                LocalTime hora = remoto.getHora();
                if (hora == null) {
                    hora = LocalTime.of(0, 0); // requerido por entidad local
                    log.warn("⚠️  [Sync-Eventos] El evento {} no tiene hora en el proxy. Se asigna 00:00.", remoto.getId());
                }
                local.setHora(hora);

                // ASIENTOS (filas y columnas)
                Integer filas = remoto.getFilaAsientos();
                Integer columnas = remoto.getColumnaAsientos();

                // Validación estricta según reglas del dominio
                if (filas == null || columnas == null || filas <= 0 || columnas <= 0) {
                    log.error(
                        "❌ [Sync-Eventos] Evento {} tiene datos inválidos de asientos (filas={}, cols={}). Evento NO sincronizado.",
                        remoto.getId(), filas, columnas
                    );
                    continue; // NO guardar en BD
                }

                local.setFilaAsientos(filas);
                local.setColumnaAsientos(columnas);
                local.setCantidadAsientosTotales(filas * columnas);

                // PRECIO DE ENTRADA
                BigDecimal precioEntrada = remoto.getPrecioEntrada();
                if (precioEntrada == null) {
                    log.warn(
                        "⚠️  [Sync-Eventos] El evento {} no tiene precioEntrada en el proxy. Se asigna 0.",
                        remoto.getId()
                    );
                    precioEntrada = BigDecimal.ZERO;
                } else if (precioEntrada.compareTo(BigDecimal.ZERO) < 0) {
                    log.error(
                        "❌ [Sync-Eventos] Evento {} tiene precioEntrada negativo ({}). Evento NO sincronizado.",
                        remoto.getId(),
                        precioEntrada
                    );
                    continue; // No guardamos un evento con precio inválido
                }
                local.setPrecioEntrada(precioEntrada);

                // DATOS GENERALES
                local.setTitulo(remoto.getTitulo());
                local.setDescripcion(remoto.getDescripcion());
                local.setOrganizador(remoto.getOrganizador());
                local.setPresentadores(remoto.getPresentadores());

                // 4. Guardar cambios en BD
                Evento eventoGuardado = eventoRepository.save(local);

                log.info(
                    "💾 [DB] Evento guardado → idLocal={}, externalId={}, titulo={}, precioEntrada={}",
                    eventoGuardado.getId(),
                    remoto.getId(),
                    eventoGuardado.getTitulo(),
                    eventoGuardado.getPrecioEntrada()
                );

                // 5. Sincronizar asientos de este evento concreto
                asientoSyncService.sincronizarAsientosDeEvento(eventoGuardado, remoto.getId());
            }

            // 6. Marcar como inactivos los eventos que ya no vengan desde la cátedra
            List<Evento> eventosConExternalId = eventoRepository.findByExternalIdIsNotNull();

            for (Evento eventoLocal : eventosConExternalId) {
                Long externalId = eventoLocal.getExternalId();

                if (externalId != null
                    && !externalIdsVigentes.contains(externalId)
                    && Boolean.TRUE.equals(eventoLocal.getActivo())) {

                    eventoLocal.setActivo(false);
                    eventoRepository.save(eventoLocal);

                    log.info(
                        "🗑️  [Sync-Eventos] Evento externalId={} marcado como inactivo (idLocal={})",
                        externalId,
                        eventoLocal.getId()
                    );
                }
            }

            log.info("✅ [Sync-Eventos] Sincronización de eventos finalizada correctamente.");

        } catch (Exception e) {
            log.error("❌ [Sync-Eventos] Error procesando JSON del proxy", e);
        }
    }
}
