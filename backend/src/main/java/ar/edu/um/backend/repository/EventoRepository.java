package ar.edu.um.backend.repository;

import ar.edu.um.backend.domain.Evento;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Evento entity.
 */
@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {

    // Devuelve todos los eventos ordenados cronológicamente.
    List<Evento> findAllByOrderByFechaAscHoraAsc();

    // Devuelve todos los eventos ordenados alfabéticamente por título.
    List<Evento> findAllByOrderByTituloAsc();

    // Busca un evento en la base local usando su ID real proveniente de la cátedra (externalId).
    Optional<Evento> findByExternalId(Long externalId);
}
