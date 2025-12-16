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
    default Optional<Asiento> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Asiento> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Asiento> findAllWithEagerRelationships(Pageable pageable) {
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
}
