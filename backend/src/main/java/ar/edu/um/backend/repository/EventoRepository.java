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

    // Devuelve todos los eventos ordenados por fecha y hora, sin filtrar por activo.
    // Usado antes de implementar la lógica de inactivos.
    List<Evento> findAllByOrderByFechaAscHoraAsc();

    // Devuelve únicamente los eventos ACTIVOS ordenados cronológicamente.
    // Esto garantiza que la API del backend solo muestre eventos vigentes.
    List<Evento> findAllByActivoTrueOrderByFechaAscHoraAsc();

    // Devuelve todos los eventos ordenados alfabéticamente por título, sin filtrar por activo.
    List<Evento> findAllByOrderByTituloAsc();

    // Devuelve únicamente los eventos ACTIVOS ordenados por título.
    List<Evento> findAllByActivoTrueOrderByTituloAsc();

    // Busca un evento en la base local usando su ID real proveniente de la cátedra (externalId).
    // Este metodo es esencial para la sincronización.

    // Busca un evento activo por ID.
    Optional<Evento> findByIdAndActivoTrue(Long id);

    // Devuelve solo los eventos activos sin ningún orden específico.
    List<Evento> findAllByActivoTrue();

    Optional<Evento> findByExternalId(Long externalId);

    // Devuelve todos los eventos que poseen externalId asignado
    // Es decir, que provienen de la cátedra y no son creados manualmente.
    // Útil para procesos de sincronización y validación.
    List<Evento> findByExternalIdIsNotNull();

}
