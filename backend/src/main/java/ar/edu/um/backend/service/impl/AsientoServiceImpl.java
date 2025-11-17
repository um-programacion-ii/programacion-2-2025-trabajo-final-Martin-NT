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
        LOG.debug("Request to save Asiento : {}", asientoDTO);
        Asiento asiento = asientoMapper.toEntity(asientoDTO);
        asiento = asientoRepository.save(asiento);
        return asientoMapper.toDto(asiento);
    }

    @Override
    public AsientoDTO update(AsientoDTO asientoDTO) {
        LOG.debug("Request to update Asiento : {}", asientoDTO);
        Asiento asiento = asientoMapper.toEntity(asientoDTO);
        asiento = asientoRepository.save(asiento);
        return asientoMapper.toDto(asiento);
    }

    @Override
    public Optional<AsientoDTO> partialUpdate(AsientoDTO asientoDTO) {
        LOG.debug("Request to partially update Asiento : {}", asientoDTO);

        return asientoRepository
            .findById(asientoDTO.getId())
            .map(existingAsiento -> {
                asientoMapper.partialUpdate(existingAsiento, asientoDTO);

                return existingAsiento;
            })
            .map(asientoRepository::save)
            .map(asientoMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsientoDTO> findAll() {
        LOG.debug("Request to get all Asientos");
        return asientoRepository.findAll().stream().map(asientoMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    public Page<AsientoDTO> findAllWithEagerRelationships(Pageable pageable) {
        return asientoRepository.findAllWithEagerRelationships(pageable).map(asientoMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AsientoDTO> findOne(Long id) {
        LOG.debug("Request to get Asiento : {}", id);
        return asientoRepository.findOneWithEagerRelationships(id).map(asientoMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Asiento : {}", id);
        asientoRepository.deleteById(id);
    }
}
