package ar.edu.um.backend.service;

import ar.edu.um.backend.domain.Evento;
import ar.edu.um.backend.repository.EventoRepository;
import ar.edu.um.backend.service.dto.ProxyEventoDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
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
 *   - el proxy notifica cambios (Kafka → proxy → backend),
 *
 * El proceso consiste en:
 *    1. Obtener el JSON real de eventos desde el proxy.
 *    2. Convertir el JSON en ProxyEventoDTO.
 *    3. Por cada evento:
 *        - si no existe en la BD → se crea
 *        - si existe → se actualiza
 *    4. Aplicar valores por defecto cuando la cátedra no provee campos.
 *    5. Marcar como inactivos los eventos locales que ya no vengan del proxy.
 *
 * Este servicio solo maneja la tabla Evento.
 */
@Service
@Transactional  // Garantiza atomicidad: si falla algo → rollback de cambios
public class EventoSyncService {

    private static final Logger log = LoggerFactory.getLogger(EventoSyncService.class);

    private final ProxyService proxyService;          // Cliente que consulta al proxy
    private final EventoRepository eventoRepository; // Acceso a la base local
    private final ObjectMapper objectMapper;          // Convierte JSON → objetos Java

    public EventoSyncService(ProxyService proxyService, EventoRepository eventoRepository, ObjectMapper objectMapper) {
        this.proxyService = proxyService;
        this.eventoRepository = eventoRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Sincroniza los eventos locales con los datos provenientes del proxy.
     * Metodo central de sincronización.
     */
    public void sincronizarEventosDesdeProxy() {

        log.info("🔄 [Sync] Iniciando sincronización de eventos contra proxy...");

        // 1. Obtener JSON desde el proxy
        String json = proxyService.listarEventosCompletos();

        if (json == null) {
            log.warn("⚠️  [Sync] No se pudo obtener la lista de eventos desde el proxy. Cancelando sincronización.");
            return;
        }

        try {
            // 2. Convertir JSON → lista de eventos remotos
            ProxyEventoDTO[] remotosArray = objectMapper.readValue(json, ProxyEventoDTO[].class);
            List<ProxyEventoDTO> remotos = Arrays.asList(remotosArray);

            log.info("📥 [Sync] Eventos recibidos desde proxy: {} evento(s).", remotos.size());

            // Conjunto de IDs externos que siguen "vivos" en la cátedra
            Set<Long> externalIdsVigentes = new HashSet<>();

            // 3. Procesar evento por evento
            for (ProxyEventoDTO remoto : remotos) {

                // Validación mínima: todo evento debe tener un ID externo
                if (remoto.getId() == null) {
                    log.warn("⚠️  [Sync] Evento remoto ignorado (sin ID). Título={}", remoto.getTitulo());
                    continue;
                }

                externalIdsVigentes.add(remoto.getId());

                // Buscar si ya existe un evento con ese externalId
                Optional<Evento> optLocal = eventoRepository.findByExternalId(remoto.getId());
                Evento local = optLocal.orElseGet(Evento::new);

                // Crear o actualizar
                if (local.getId() == null) {
                    log.info("🆕 [Sync] Creando evento nuevo (externalId={}) → {}", remoto.getId(), remoto.getTitulo());
                    local.setExternalId(remoto.getId());
                } else {
                    log.info(
                        "♻️  [Sync] Actualizando evento existente (id={}, externalId={}) → {}",
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
                        "⚠️  [Sync] El evento {} no tiene fecha en el proxy. Se asigna fecha actual: {}",
                        remoto.getId(),
                        fecha
                    );
                }
                local.setFecha(fecha);

                // HORA
                LocalTime hora = remoto.getHora();
                if (hora == null) {
                    hora = LocalTime.of(0, 0); // requerido por entidad local
                    log.warn("⚠️  [Sync] El evento {} no tiene hora en el proxy. Se asigna 00:00.", remoto.getId());
                }
                local.setHora(hora);

                // ASIENTOS (filas y columnas)
                Integer filas = remoto.getFilaAsientos();
                Integer columnas = remoto.getColumnaAsientos();

               // Validación estricta según reglas del dominio
                if (filas == null || columnas == null || filas <= 0 || columnas <= 0) {
                    log.error(
                        "❌ [Sync] Evento {} tiene datos inválidos de asientos (filas={}, cols={}). Evento NO sincronizado.",
                        remoto.getId(), filas, columnas
                    );
                    continue; // NO guardar en BD
                }

                local.setFilaAsientos(filas);
                local.setColumnaAsientos(columnas);
                local.setCantidadAsientosTotales(filas * columnas);


                // DATOS GENERALES
                local.setTitulo(remoto.getTitulo());
                local.setDescripcion(remoto.getDescripcion());
                local.setOrganizador(remoto.getOrganizador());
                local.setPresentadores(remoto.getPresentadores());

                // 4. Guardar cambios en BD
                eventoRepository.save(local);

                log.info(
                    "💾 [DB] Evento guardado → idLocal={}, externalId={}, titulo={}",
                    local.getId(),
                    remoto.getId(),
                    local.getTitulo()
                );
            }

            // 5. Marcar como inactivos los eventos que ya no vengan desde la cátedra
            List<Evento> eventosConExternalId = eventoRepository.findByExternalIdIsNotNull();

            for (Evento eventoLocal : eventosConExternalId) {
                Long externalId = eventoLocal.getExternalId();

                if (externalId != null
                    && !externalIdsVigentes.contains(externalId)
                    && Boolean.TRUE.equals(eventoLocal.getActivo())) {

                    eventoLocal.setActivo(false);
                    eventoRepository.save(eventoLocal);

                    log.info(
                        "🗑️  [Sync] Evento externalId={} marcado como inactivo (idLocal={})",
                        externalId,
                        eventoLocal.getId()
                    );
                }
            }

            log.info("✅ [Sync] Sincronización finalizada correctamente.");

        } catch (Exception e) {
            log.error("❌ [Sync] Error procesando JSON del proxy", e);
        }
    }
}
