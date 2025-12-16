package ar.edu.um.backend.service;

import ar.edu.um.backend.service.dto.EventoDTO;
import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing {@link ar.edu.um.backend.domain.Evento}.
 */
public interface EventoService {
    /**
     * Save a evento.
     *
     * @param eventoDTO the entity to save.
     * @return the persisted entity.
     */
    EventoDTO save(EventoDTO eventoDTO);

    /**
     * Updates a evento.
     *
     * @param eventoDTO the entity to update.
     * @return the persisted entity.
     */
    EventoDTO update(EventoDTO eventoDTO);

    /**
     * Partially updates a evento.
     *
     * @param eventoDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<EventoDTO> partialUpdate(EventoDTO eventoDTO);

    /**
     * Get all the eventos.
     *
     * @return the list of entities.
     */
    List<EventoDTO> findAll();

    /**
     * Get the "id" evento.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<EventoDTO> findOne(Long id);

    /**
     * Delete the "id" evento.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
