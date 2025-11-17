package ar.edu.um.backend.repository;

import ar.edu.um.backend.domain.Venta;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Venta entity.
 *
 * When extending this class, extend VentaRepositoryWithBagRelationships too.
 * For more information refer to https://github.com/jhipster/generator-jhipster/issues/17990.
 */
@Repository
public interface VentaRepository extends VentaRepositoryWithBagRelationships, JpaRepository<Venta, Long> {
    default Optional<Venta> findOneWithEagerRelationships(Long id) {
        return this.fetchBagRelationships(this.findOneWithToOneRelationships(id));
    }

    default List<Venta> findAllWithEagerRelationships() {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships());
    }

    default Page<Venta> findAllWithEagerRelationships(Pageable pageable) {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships(pageable));
    }

    @Query(value = "select venta from Venta venta left join fetch venta.evento", countQuery = "select count(venta) from Venta venta")
    Page<Venta> findAllWithToOneRelationships(Pageable pageable);

    @Query("select venta from Venta venta left join fetch venta.evento")
    List<Venta> findAllWithToOneRelationships();

    @Query("select venta from Venta venta left join fetch venta.evento where venta.id =:id")
    Optional<Venta> findOneWithToOneRelationships(@Param("id") Long id);
}
