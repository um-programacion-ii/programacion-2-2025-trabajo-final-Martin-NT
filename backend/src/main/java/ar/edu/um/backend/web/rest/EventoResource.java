package ar.edu.um.backend.web.rest;
import ar.edu.um.backend.repository.EventoRepository;
import ar.edu.um.backend.security.AuthoritiesConstants;
import ar.edu.um.backend.service.AsientoBloqueoService;
import ar.edu.um.backend.service.AsientoEstadoService;
import ar.edu.um.backend.service.EventoService;
import ar.edu.um.backend.service.EventoSyncService;
import ar.edu.um.backend.service.dto.*;
import ar.edu.um.backend.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link ar.edu.um.backend.domain.Evento}.
 *
 * Además de los endpoints CRUD generados por JHipster, expone:
 *  - GET  /api/eventos/{id}/asientos  → estado de asientos en tiempo real
 *    (DB local + Redis vía proxy).
 *  - POST /api/eventos/{id}/bloqueos → bloqueo de asientos (Payload 6).
 *
 * También administra la sincronización manual de eventos entre:
 *   Backend ←→ Proxy-Service ←→ Servidor de la cátedra.
 */
@RestController
@RequestMapping("/api/eventos")
public class EventoResource {

    private static final Logger LOG = LoggerFactory.getLogger(EventoResource.class);

    private static final String ENTITY_NAME = "evento";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final EventoService eventoService;
    private final EventoRepository eventoRepository;
    private final AsientoEstadoService asientoEstadoService;
    private final AsientoBloqueoService asientoBloqueoService;
    private final EventoSyncService eventoSyncService;

    public EventoResource(EventoService eventoService, EventoRepository eventoRepository, AsientoEstadoService asientoEstadoService, AsientoBloqueoService asientoBloqueoService, EventoSyncService eventoSyncService) {
        this.eventoService = eventoService;
        this.eventoRepository = eventoRepository;
        this.asientoEstadoService = asientoEstadoService;
        this.asientoBloqueoService = asientoBloqueoService;
        this.eventoSyncService = eventoSyncService;
    }

    /**
     * {@code POST  /eventos} : Create a new evento.
     *
     * @param eventoDTO the eventoDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new eventoDTO, or with status {@code 400 (Bad Request)} if the evento has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<EventoDTO> createEvento(@Valid @RequestBody EventoDTO eventoDTO) throws URISyntaxException {
        LOG.debug("REST request to save Evento : {}", eventoDTO);
        if (eventoDTO.getId() != null) {
            throw new BadRequestAlertException("A new evento cannot already have an ID", ENTITY_NAME, "idexists");
        }
        eventoDTO = eventoService.save(eventoDTO);
        return ResponseEntity.created(new URI("/api/eventos/" + eventoDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, eventoDTO.getId().toString()))
            .body(eventoDTO);
    }

    /**
     * {@code PUT  /eventos/:id} : Updates an existing evento.
     *
     * @param id the id of the eventoDTO to save.
     * @param eventoDTO the eventoDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated eventoDTO,
     * or with status {@code 400 (Bad Request)} if the eventoDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the eventoDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EventoDTO> updateEvento(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody EventoDTO eventoDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Evento : {}, {}", id, eventoDTO);
        if (eventoDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, eventoDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!eventoRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        eventoDTO = eventoService.update(eventoDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, eventoDTO.getId().toString()))
            .body(eventoDTO);
    }

    /**
     * {@code PATCH  /eventos/:id} : Partial updates given fields of an existing evento, field will ignore if it is null
     *
     * @param id the id of the eventoDTO to save.
     * @param eventoDTO the eventoDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated eventoDTO,
     * or with status {@code 400 (Bad Request)} if the eventoDTO is not valid,
     * or with status {@code 404 (Not Found)} if the eventoDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the eventoDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<EventoDTO> partialUpdateEvento(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody EventoDTO eventoDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Evento partially : {}, {}", id, eventoDTO);
        if (eventoDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, eventoDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!eventoRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<EventoDTO> result = eventoService.partialUpdate(eventoDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, eventoDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /eventos} : get all the eventos.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of eventos in body.
     */
    @GetMapping("")
    public List<EventoDTO> getAllEventos() {
        LOG.info("[EventoResource] GET /api/eventos (devolviendo solo eventos activos)");
        return eventoService.findAll();
    }

    /**
     * {@code DELETE  /eventos/:id} : delete the "id" evento.
     *
     * @param id the id of the eventoDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvento(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Evento : {}", id);
        eventoService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code GET  /eventos/completos} : get all the eventos con información completa.
     *
     * @return
     */
    @GetMapping("/completos")
    public List<ProxyEventoDetalleDTO> getAllEventosCompletos() {
        LOG.info("[EventoResource] GET /api/eventos/completos (devolviendo solo eventos completos)");
        return eventoService.findAllCompletos();
    }

    /**
     * {@code GET  /eventos/resumidos} : get all the eventos con información resumida.
     *
     * @return
     */
    @GetMapping("/resumidos")
    public List<ProxyEventoResumenDTO> getAllEventosResumidos() {
        LOG.info("[EventoResource] GET /api/eventos/resumidos (devolviendo solo eventos resumiidos)");
        return eventoService.findAllResumidos();
    }

    /**
     * {@code GET  /eventos/eventos}/id} : get the "id" evento.
     *
     * @return
     */
    @GetMapping("/{id}")
    public ProxyEventoDetalleDTO getEventoId(@PathVariable("id") Long id) {
        LOG.info("[EventoResource] GET /api/eventos/{}", id);
        return eventoService.findOneById(id);
    }


    /**
     * {@code GET  /eventos/:id} : get the "id" evento.
     *
     * @param id the id of the eventoDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the eventoDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/locales/{id}")
    public ResponseEntity<EventoDTO> getEventoLocal(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Active Evento : {}", id);
        Optional<EventoDTO> eventoDTO = eventoService.findOne(id);
        return ResponseUtil.wrapOrNotFound(eventoDTO);
    }

    /**
     * GET  /api/eventos/{id}/asientos
     *
     * Devuelve el mapa de asientos del evento en tiempo real, (estado-asientos-tiempo-real)
     * usando Redis como fuente de verdad y completando LIBRES por diferencia.
     */
    @GetMapping("/{id}/asientos")
    public ResponseEntity<List<AsientoEstadoDTO>> obtenerEstadoActualAsientos(@PathVariable Long id) {
        LOG.info("[EventoResource] GET /api/eventos/{}/asientos (mapa en tiempo real)", id);

        List<AsientoEstadoDTO> mapa = asientoEstadoService.obtenerEstadoActualDeAsientos(id);

        return ResponseEntity.ok(mapa);
    }

    /**
     * POST /api/eventos/{id}/bloqueos
     *
     * Solicita el bloqueo de uno o más asientos para un evento local.
     *
     * El bloqueo es "todo o nada":
     * - Si todos los asientos pueden bloquearse → resultado=true.
     * - Si alguno está ocupado o bloqueado → resultado=false con detalle por asiento.
     *
     * Body (Payload 6):
     * {
     *   "eventoId": 1,
     *   "asientos": [
     *     { "fila": 2, "columna": 1 },
     *     { "fila": 2, "columna": 2 }
     *   ]
     * }
     *
     * Respuesta (ejemplo):
     * {
     *   "resultado": false,
     *   "descripcion": "No todos los asientos pueden ser bloqueados",
     *   "eventoId": 1,
     *   "asientos": [
     *     { "fila": 2, "columna": 1, "estado": "Ocupado" },
     *     { "fila": 2, "columna": 2, "estado": "Bloqueado" }
     *   ]
     * }
     */
    @PostMapping("/{id}/bloqueos")
    public ResponseEntity<AsientoBloqueoResponseDTO> bloquearAsientos(
        @PathVariable("id") Long eventoIdLocal,
        @RequestBody AsientoBloqueoRequestDTO request
    ) {
        LOG.info("🔒 [Bloqueo] POST /api/eventos/{}/bloqueos body={}", eventoIdLocal, request);

        try {
            AsientoBloqueoResponseDTO respuesta =
                asientoBloqueoService.bloquearAsientos(eventoIdLocal, request);

            return ResponseEntity.ok(respuesta);

        } catch (IllegalStateException e) {
            LOG.warn(
                "⚠️ [Bloqueo] Error de negocio para eventoIdLocal={}: {}",
                eventoIdLocal,
                e.getMessage()
            );
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }


    /**
     * POST /api/eventos/sync-eventos
     * ---------------------------------------------------------
     * Este endpoint:
     *   - Dispara una sincronización manual de eventos.
     *   - Llama internamente a EventoSyncService.sincronizarEventosDesdeProxy().
     *   - Reemplaza datos locales con los datos reales obtenidos desde la cátedra.
     *
     * Respuesta:
     *   204 No Content → Se ejecutó correctamente pero no devuelve body.
     */
    @PostMapping("/sync-eventos")
    public ResponseEntity<Void> syncEventosManualmente() {

        LOG.info("[Admin-Sync] Solicitud manual de sincronización de eventos.");

        // Ejecuta la sincronización real (acceso al proxy + persistencia local)
        eventoSyncService.sincronizarEventosDesdeProxy();

        LOG.info("[Admin-Sync] Sincronización manual finalizada.");

        return ResponseEntity.noContent().build(); // HTTP 204
    }
}
