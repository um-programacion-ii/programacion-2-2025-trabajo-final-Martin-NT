package ar.edu.um.backend.service.impl;

import ar.edu.um.backend.domain.Evento;
import ar.edu.um.backend.repository.EventoRepository;
import ar.edu.um.backend.service.EventoService;
import ar.edu.um.backend.service.dto.EventoDTO;
import ar.edu.um.backend.service.mapper.EventoMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import ar.edu.um.backend.web.rest.errors.EventoAsientosInvalidosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link ar.edu.um.backend.domain.Evento}.
 */
@Service
@Transactional
public class EventoServiceImpl implements EventoService {

    private static final Logger LOG = LoggerFactory.getLogger(EventoServiceImpl.class);

    private final EventoRepository eventoRepository;

    private final EventoMapper eventoMapper;

    public EventoServiceImpl(EventoRepository eventoRepository, EventoMapper eventoMapper) {
        this.eventoRepository = eventoRepository;
        this.eventoMapper = eventoMapper;
    }

    @Override
    public EventoDTO save(EventoDTO eventoDTO) {
        LOG.debug("Solicitud para guardar Evento : {}", eventoDTO);
        Evento evento = eventoMapper.toEntity(eventoDTO);

        // Si viene sin info, por defecto se considera activo
        if (evento.getActivo() == null) {
            evento.setActivo(true);
        }

        // Lógica de dominio: validar y recalcular cantidadAsientosTotales
        validateAndRecalculateAsientos(evento);

        evento = eventoRepository.save(evento);
        return eventoMapper.toDto(evento);
    }

    @Override
    public EventoDTO update(EventoDTO eventoDTO) {
        LOG.debug("Solicitud para actualizar Evento : {}", eventoDTO);
        Evento evento = eventoMapper.toEntity(eventoDTO);

        if (evento.getActivo() == null) {
            evento.setActivo(true);
        }

        validateAndRecalculateAsientos(evento);

        evento = eventoRepository.save(evento);
        return eventoMapper.toDto(evento);
    }

    @Override
    public Optional<EventoDTO> partialUpdate(EventoDTO eventoDTO) {
        LOG.debug("Solicitud para actualizar parcialmente Evento ACTIVO : {}", eventoDTO);

        return eventoRepository
            .findByIdAndActivoTrue(eventoDTO.getId())   // 👈 sólo si está activo
            .map(existingEvento -> {
                // PATCH: actualiza solo los campos enviados en el DTO (parche parcial).
                // Los campos null no se modifican. Aplica la regla de negocio si filas/columnas están presentes.
                eventoMapper.partialUpdate(existingEvento, eventoDTO);

                // Si ya tenemos fila y columna, aplicamos la lógica de dominio
                if (existingEvento.getFilaAsientos() != null && existingEvento.getColumnaAsientos() != null) {
                    validateAndRecalculateAsientos(existingEvento);
                }

                return existingEvento;
            })
            .map(eventoRepository::save)
            .map(eventoMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoDTO> findAll() {
        LOG.debug("Solicitud para obtener todos los Eventos ACTIVOS");
        return eventoRepository
            .findAllByActivoTrueOrderByFechaAscHoraAsc()
            .stream()
            .map(eventoMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EventoDTO> findOne(Long id) {
        LOG.debug("Solicitud para obtener Evento ACTIVO : {}", id);
        return eventoRepository.findByIdAndActivoTrue(id).map(eventoMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Solicitud para eliminar Evento : {}", id);
        eventoRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoDTO> findAllOrderedByFechaHora() {
        LOG.debug("Solicitud para obtener todos los Eventos ACTIVOS ordenados por fecha/hora");
        return eventoRepository
            .findAllByActivoTrueOrderByFechaAscHoraAsc()
            .stream()
            .map(eventoMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoDTO> findAllByOrderByTituloAsc() {
        LOG.debug("Solicitud para obtener todos los Eventos ACTIVOS ordenados por título");
        return eventoRepository
            .findAllByActivoTrueOrderByTituloAsc()
            .stream()
            .map(eventoMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }


    // Metodo que valida que filaAsientos y columnaAsientos sean ≠ null & > 0
    // y recalcula cantidadAsientosTotales = filaAsientos × columnaAsientos
    private void validateAndRecalculateAsientos(Evento evento) {
        Integer filas = evento.getFilaAsientos();
        Integer columnas = evento.getColumnaAsientos();

        if (filas == null || columnas == null) {
            throw new EventoAsientosInvalidosException("filaAsientos y columnaAsientos no pueden ser nulos");
        }

        if (filas <= 0 || columnas <= 0) {
            throw new EventoAsientosInvalidosException("filaAsientos y columnaAsientos deben ser mayores a cero");
        }

        int total = filas * columnas;
        evento.setCantidadAsientosTotales(total);
    }
}
