package ar.edu.um.backend.repository;

import ar.edu.um.backend.domain.Asiento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Asiento entity.
 */
@Repository
public interface AsientoRepository extends JpaRepository<Asiento, Long> {

    // Métodos generados por JHipster
    // para traer Asientos junto con su Evento (relación ManyToOne)
    default Optional<Asiento> findOneWithEagerRelationships(Long id) {
        // Obtiene un asiento cargando también su evento (fetch join)
        return this.findOneWithToOneRelationships(id);
    }

    default List<Asiento> findAllWithEagerRelationships() {
        // Obtiene todos los asientos cargando también su evento
        return this.findAllWithToOneRelationships();
    }

    default Page<Asiento> findAllWithEagerRelationships(Pageable pageable) {
        // Obtiene todos los asientos paginados con su evento asociado
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select asiento from Asiento asiento left join fetch asiento.evento",
        countQuery = "select count(asiento) from Asiento asiento"
    )
    Page<Asiento> findAllWithToOneRelationships(Pageable pageable);

    @Query("select asiento from Asiento asiento left join fetch asiento.evento")
    List<Asiento> findAllWithToOneRelationships();

    @Query("select asiento from Asiento asiento left join fetch asiento.evento where asiento.id =:id")
    Optional<Asiento> findOneWithToOneRelationships(@Param("id") Long id);

    // Devuelve todos los asientos de un evento específico,
    // Ordenados por fila ascendente y columna ascendente.
    List<Asiento> findByEventoIdOrderByFilaAscColumnaAsc(Long eventoId);

}
