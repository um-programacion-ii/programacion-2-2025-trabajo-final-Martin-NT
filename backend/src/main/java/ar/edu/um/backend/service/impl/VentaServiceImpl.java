package ar.edu.um.backend.service.impl;

import ar.edu.um.backend.domain.Asiento;
import ar.edu.um.backend.domain.Venta;
import ar.edu.um.backend.domain.enumeration.AsientoEstado;
import ar.edu.um.backend.domain.enumeration.VentaEstado;
import ar.edu.um.backend.repository.AsientoRepository;
import ar.edu.um.backend.repository.VentaRepository;
import ar.edu.um.backend.service.VentaService;
import ar.edu.um.backend.service.dto.VentaDTO;
import ar.edu.um.backend.service.mapper.VentaMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import ar.edu.um.backend.web.rest.errors.VentaInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link ar.edu.um.backend.domain.Venta}.
 */
@Service
@Transactional
public class VentaServiceImpl implements VentaService {

    private static final Logger LOG = LoggerFactory.getLogger(VentaServiceImpl.class);

    private final VentaRepository ventaRepository;
    private final AsientoRepository asientoRepository;

    private final VentaMapper ventaMapper;

    public VentaServiceImpl(VentaRepository ventaRepository, AsientoRepository asientoRepository, VentaMapper ventaMapper) {
        this.ventaRepository = ventaRepository;
        this.asientoRepository = asientoRepository;
        this.ventaMapper = ventaMapper;
    }

    @Override
    public VentaDTO save(VentaDTO ventaDTO) {
        LOG.debug("Solicitud para guardar Venta : {}", ventaDTO);
        Venta venta = ventaMapper.toEntity(ventaDTO);

        validateVenta(venta);
        procesarVentaExitosa(venta);

        venta = ventaRepository.save(venta);
        return ventaMapper.toDto(venta);
    }

    @Override
    public VentaDTO update(VentaDTO ventaDTO) {
        LOG.debug("Solicitud para actualizar Venta : {}", ventaDTO);
        Venta venta = ventaMapper.toEntity(ventaDTO);

        validateVenta(venta);
        procesarVentaExitosa(venta);

        venta = ventaRepository.save(venta);
        return ventaMapper.toDto(venta);
    }

    @Override
    public Optional<VentaDTO> partialUpdate(VentaDTO ventaDTO) {
        LOG.debug("Solicitud para actualizar parcialmente Venta : {}", ventaDTO);

        return ventaRepository
            .findById(ventaDTO.getId())
            .map(existingVenta -> {
                // PATCH: solo actualiza campos no nulos del DTO
                ventaMapper.partialUpdate(existingVenta, ventaDTO);

                validateVenta(existingVenta);
                procesarVentaExitosa(existingVenta);

                return existingVenta;
            })
            .map(ventaRepository::save)
            .map(ventaMapper::toDto);
    }


    @Override
    @Transactional(readOnly = true)
    public List<VentaDTO> findAll() {
        LOG.debug("Solicitud para obtener todas las Ventas");
        return ventaRepository.findAll()
            .stream()
            .map(ventaMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VentaDTO> findAllWithEagerRelationships(Pageable pageable) {
        LOG.debug("Solicitud para obtener todas las Ventas (relaciones eager)");
        return ventaRepository.findAllWithEagerRelationships(pageable)
            .map(ventaMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VentaDTO> findOne(Long id) {
        LOG.debug("Solicitud para obtener la Venta con id: {}", id);
        return ventaRepository.findOneWithEagerRelationships(id)
            .map(ventaMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Solicitud para eliminar la Venta con id: {}", id);
        ventaRepository.deleteById(id);
    }


    /**
     * Valida las reglas de dominio de una Venta antes de hacerla.
     *
     * - precioVenta > 0
     * - cantidadAsientos > 0
     * - cantidadAsientos coincide con la cantidad de asientos asociados
     * - todos los asientos pertenecen al mismo evento de la venta
     * - ningún asiento está ya en estado VENDIDO
     */
    private void validateVenta(Venta venta) {
        if (venta.getPrecioVenta() == null || venta.getPrecioVenta().signum() <= 0) {
            throw new VentaInvalidaException("El precio de la venta debe ser mayor a cero");
        }

        if (venta.getCantidadAsientos() == null || venta.getCantidadAsientos() <= 0) {
            throw new VentaInvalidaException("La cantidad de asientos debe ser mayor a cero");
        }

        if (venta.getAsientos() == null || venta.getAsientos().isEmpty()) {
            throw new VentaInvalidaException("La venta debe contener al menos un asiento");
        }

        // Validar que cantidadAsientos coincide con el número de asientos asociados
        int asientosSize = venta.getAsientos().size();
        if (!venta.getCantidadAsientos().equals(asientosSize)) {
            throw new VentaInvalidaException(
                "La cantidadAsientos no coincide con la cantidad de asientos asociados ("
                    + venta.getCantidadAsientos()
                    + " vs "
                    + asientosSize
                    + ")"
            );
        }

        // Validar que todos los asientos pertenecen al mismo evento
        Long eventoIdVenta = venta.getEvento() != null ? venta.getEvento().getId() : null;
        if (eventoIdVenta == null) {
            throw new VentaInvalidaException("La venta debe estar asociada a un evento");
        }

        for (Asiento asiento : venta.getAsientos()) {
            if (asiento.getEvento() == null || asiento.getEvento().getId() == null) {
                throw new VentaInvalidaException("Todos los asientos deben tener un evento asociado");
            }
            if (!eventoIdVenta.equals(asiento.getEvento().getId())) {
                throw new VentaInvalidaException("Todos los asientos deben pertenecer al mismo evento de la venta");
            }

            // No se puede vender un asiento que ya está vendido
            if (asiento.getEstado() == AsientoEstado.VENDIDO) {
                throw new VentaInvalidaException("No se puede incluir un asiento ya vendido en la venta");
            }
        }
    }

    /**
     * Aplica las reglas de post-procesamiento cuando la venta es EXITOSA:
     * - Marca todos los asientos asociados como VENDIDO.
     */
    private void procesarVentaExitosa(Venta venta) {
        if (venta.getEstado() != VentaEstado.EXITOSA) {
            return; // si no es EXITOSA, no hacemos nada
        }

        for (Asiento asiento : venta.getAsientos()) {
            asiento.setEstado(AsientoEstado.VENDIDO);
            // asiento.setPersonaActual(null); // Para limpiar personaActual
        }

        asientoRepository.saveAll(venta.getAsientos());
    }


}
