package ar.edu.um.backend.web.rest;
import ar.edu.um.backend.domain.Venta;
import ar.edu.um.backend.repository.VentaRepository;
import ar.edu.um.backend.service.VentaService;
import ar.edu.um.backend.service.VentaSyncService;
import ar.edu.um.backend.service.dto.ProxyVentaResponseDTO;
import ar.edu.um.backend.service.dto.VentaDTO;
import ar.edu.um.backend.service.dto.VentaRequestFrontendDTO;
import ar.edu.um.backend.service.mapper.VentaMapper;
import ar.edu.um.backend.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;
/**
 * Controlador REST encargado de gestionar las operaciones relacionadas
 * con la entidad {@link ar.edu.um.backend.domain.Venta}.
 *
 * Expone:
 * - Endpoints CRUD tradicionales para administrar ventas locales.
 * - Un endpoint especial para iniciar una venta real de asientos,
 *   validando bloqueos en Redis y confirmando la operación con la cátedra
 *   a través del proxy.
 */
@RestController
@RequestMapping("/api/ventas")
public class VentaResource {

    private static final Logger LOG = LoggerFactory.getLogger(VentaResource.class);
    private static final String ENTITY_NAME = "venta";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final VentaService ventaService;
    private final VentaRepository ventaRepository;
    private final VentaSyncService ventaSyncService;
    private final VentaMapper ventaMapper;

    public VentaResource(
        VentaService ventaService,
        VentaRepository ventaRepository,
        VentaSyncService ventaSyncService,
        VentaMapper ventaMapper
    ) {
        this.ventaService = ventaService;
        this.ventaRepository = ventaRepository;
        this.ventaSyncService = ventaSyncService;
        this.ventaMapper = ventaMapper;
    }

    /**
     * {@code POST  /ventas} : Create a new venta.
     */
    @PostMapping("")
    public ResponseEntity<VentaDTO> createVenta(@Valid @RequestBody VentaDTO ventaDTO) throws URISyntaxException {
        LOG.debug("REST request to save Venta : {}", ventaDTO);
        if (ventaDTO.getId() != null) {
            throw new BadRequestAlertException("A new venta cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ventaDTO = ventaService.save(ventaDTO);
        return ResponseEntity.created(new URI("/api/ventas/" + ventaDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, ventaDTO.getId().toString()))
            .body(ventaDTO);
    }

    /**
     * {@code PUT  /ventas/:id} : Updates an existing venta.
     */
    @PutMapping("/{id}")
    public ResponseEntity<VentaDTO> updateVenta(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody VentaDTO ventaDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Venta : {}, {}", id, ventaDTO);
        if (ventaDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, ventaDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!ventaRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        ventaDTO = ventaService.update(ventaDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, ventaDTO.getId().toString()))
            .body(ventaDTO);
    }

    /**
     * {@code PATCH  /ventas/:id} : Partial updates given fields of an existing venta.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<VentaDTO> partialUpdateVenta(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody VentaDTO ventaDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Venta partially : {}, {}", id, ventaDTO);
        if (ventaDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, ventaDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!ventaRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<VentaDTO> result = ventaService.partialUpdate(ventaDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, ventaDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /ventas} : get all the ventas.
     */
    @GetMapping("")
    public List<VentaDTO> getAllVentas(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all Ventas");
        return ventaService.findAll();
    }

    /**
     * {@code GET  /ventas/:id} : get the "id" venta.
     */
    @GetMapping("/{id}")
    public ResponseEntity<VentaDTO> getVenta(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Venta : {}", id);
        Optional<VentaDTO> ventaDTO = ventaService.findOne(id);
        return ResponseUtil.wrapOrNotFound(ventaDTO);
    }

    /**
     * {@code DELETE  /ventas/:id} : delete the "id" venta.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVenta(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Venta : {}", id);
        ventaService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }


    /**
     * POST /api/ventas/eventos/{eventoId}/venta :
     * Crea una venta real para un evento, validando bloqueos en Redis
     * y confirmando la operación con la cátedra.
     */
    @PostMapping("/eventos/{eventoId}/venta")
    public ResponseEntity<ProxyVentaResponseDTO> crearVentaParaEvento(
        @PathVariable Long eventoId,
        @Valid @RequestBody VentaRequestFrontendDTO request
    ) {
        LOG.info("[Venta] Solicitud de venta recibida para eventoId={} (local)", eventoId);

        request.setEventoId(eventoId);

        ProxyVentaResponseDTO resp = ventaSyncService.procesarVenta(request);
        return ResponseEntity.ok(resp);
    }
}
