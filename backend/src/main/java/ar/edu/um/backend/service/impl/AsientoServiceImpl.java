package ar.edu.um.backend.service.impl;

import ar.edu.um.backend.domain.Asiento;
import ar.edu.um.backend.repository.AsientoRepository;
import ar.edu.um.backend.service.AsientoService;
import ar.edu.um.backend.service.dto.AsientoDTO;
import ar.edu.um.backend.service.mapper.AsientoMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import ar.edu.um.backend.web.rest.errors.AsientoInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link ar.edu.um.backend.domain.Asiento}.
 */
@Service
@Transactional
public class AsientoServiceImpl implements AsientoService {

    private static final Logger LOG = LoggerFactory.getLogger(AsientoServiceImpl.class);

    private final AsientoRepository asientoRepository;

    private final AsientoMapper asientoMapper;

    public AsientoServiceImpl(AsientoRepository asientoRepository, AsientoMapper asientoMapper) {
        this.asientoRepository = asientoRepository;
        this.asientoMapper = asientoMapper;
    }

    @Override
    public AsientoDTO save(AsientoDTO asientoDTO) {
        LOG.debug("Solicitud para guardar Asiento : {}", asientoDTO);
        Asiento asiento = asientoMapper.toEntity(asientoDTO);

        validateAsiento(asiento);

        asiento = asientoRepository.save(asiento);
        return asientoMapper.toDto(asiento);
    }

    @Override
    public AsientoDTO update(AsientoDTO asientoDTO) {
        LOG.debug("Solicitud para actualizar Asiento : {}", asientoDTO);
        Asiento asiento = asientoMapper.toEntity(asientoDTO);

        validateAsiento(asiento);

        asiento = asientoRepository.save(asiento);
        return asientoMapper.toDto(asiento);
    }

    @Override
    public Optional<AsientoDTO> partialUpdate(AsientoDTO asientoDTO) {
        LOG.debug("Solicitud para actualizar parcialmente Asiento : {}", asientoDTO);

        return asientoRepository
            .findById(asientoDTO.getId())
            .map(existingAsiento -> {
                asientoMapper.partialUpdate(existingAsiento, asientoDTO);

                if (existingAsiento.getFila() != null && existingAsiento.getColumna() != null) {
                    validateAsiento(existingAsiento);
                }

                return existingAsiento;
            })
            .map(asientoRepository::save)
            .map(asientoMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsientoDTO> findAll() {
        LOG.debug("Solicitud para obtener todos los Asientos");
        return asientoRepository.findAll().stream().map(asientoMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    public Page<AsientoDTO> findAllWithEagerRelationships(Pageable pageable) {
        return asientoRepository.findAllWithEagerRelationships(pageable).map(asientoMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AsientoDTO> findOne(Long id) {
        LOG.debug("Solicitud para obtener Asiento : {}", id);
        return asientoRepository.findOneWithEagerRelationships(id).map(asientoMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Solicitud para eliminar Asiento : {}", id);
        asientoRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsientoDTO> findByEventoOrdered(Long eventoId) {
        LOG.debug("Solicitud para obtener Asientos de evento {} ordenados por fila/columna", eventoId);
        return asientoRepository
            .findByEventoIdOrderByFilaAscColumnaAsc(eventoId)
            .stream()
            .map(asientoMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }


    private void validateAsiento(Asiento asiento) {
        Integer fila = asiento.getFila();
        Integer columna = asiento.getColumna();

        if (fila == null || columna == null) {
            throw new AsientoInvalidoException("fila y columna no pueden ser nulas");
        }

        if (fila <= 0 || columna <= 0) {
            throw new AsientoInvalidoException("fila y columna deben ser mayores a cero");
        }
    }

}
