package ar.edu.um.backend.service;

import ar.edu.um.backend.service.dto.AsientoDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link ar.edu.um.backend.domain.Asiento}.
 */
public interface AsientoService {
    /**
     * Save a asiento.
     *
     * @param asientoDTO the entity to save.
     * @return the persisted entity.
     */
    AsientoDTO save(AsientoDTO asientoDTO);

    /**
     * Updates a asiento.
     *
     * @param asientoDTO the entity to update.
     * @return the persisted entity.
     */
    AsientoDTO update(AsientoDTO asientoDTO);

    /**
     * Partially updates a asiento.
     *
     * @param asientoDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<AsientoDTO> partialUpdate(AsientoDTO asientoDTO);

    /**
     * Get all the asientos.
     *
     * @return the list of entities.
     */
    List<AsientoDTO> findAll();

    /**
     * Get all the asientos with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<AsientoDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" asiento.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<AsientoDTO> findOne(Long id);

    /**
     * Delete the "id" asiento.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);

    /**
     * Obtiene todos los asientos de un evento,
     * ordenados por fila y columna.
     *
     * @param eventoId el ID del evento.
     * @return la lista de asientos ordenados.
     */
    List<AsientoDTO> findByEventoOrdered(Long eventoId);

}
