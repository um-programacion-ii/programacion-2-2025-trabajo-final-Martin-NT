package ar.edu.um.backend.repository;

import ar.edu.um.backend.domain.Evento;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the Evento entity.
 */
@SuppressWarnings("unused")
@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {

    // Eventos ordenados por fecha y hora ascendente
    List<Evento> findAllByOrderByFechaAscHoraAsc();

    // Eventos ordenados por titulo ascendente
    List<Evento> findAllByOrderByTituloAsc();
}
