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
 * Repositorio JPA para la entidad {@link Asiento}.
 *
 * - Incluye métodos generados por JHipster con relaciones eager (Evento).
 * - Agrega métodos específicos para manejar asientos por evento:
 *   - listar asientos de un evento ordenados por fila/columna
 *   - borrar todos los asientos de un evento
 */
@Repository
public interface AsientoRepository extends JpaRepository<Asiento, Long> {
    /**
     * Devuelve un asiento por id cargando también su evento asociado (fetch join).
     */
    default Optional<Asiento> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }
    /**
     * Devuelve todos los asientos cargando también su evento asociado.
     */
    default List<Asiento> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }
    /**
     * Devuelve una página de asientos con su evento asociado (relación ManyToOne).
     */
    default Page<Asiento> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }
    /**
     * Consulta JPQL que trae asientos + evento usando fetch join, con soporte paginado.
     */
    @Query(
        value = "select asiento from Asiento asiento left join fetch asiento.evento",
        countQuery = "select count(asiento) from Asiento asiento"
    )
    Page<Asiento> findAllWithToOneRelationships(Pageable pageable);
    /**
     * Consulta JPQL que trae todos los asientos + evento usando fetch join.
     */
    @Query("select asiento from Asiento asiento left join fetch asiento.evento")
    List<Asiento> findAllWithToOneRelationships();
    /**
     * Consulta JPQL que trae un asiento + evento por id usando fetch join.
     */
    @Query("select asiento from Asiento asiento left join fetch asiento.evento where asiento.id =:id")
    Optional<Asiento> findOneWithToOneRelationships(@Param("id") Long id);

    // Métodos específicos
    /**
     * Devuelve todos los asientos de un evento específico,
     * ordenados por fila y columna en forma ascendente.
     *
     * @param eventoId ID del evento local (FK en Asiento)
     * @return Lista de asientos ordenados por fila y columna.
     */
    List<Asiento> findByEventoIdOrderByFilaAscColumnaAsc(Long eventoId);

    /**
     * Elimina todos los asientos asociados a un evento.
     *
     * @param eventoId ID del evento local.
     * @return Cantidad de asientos eliminados.
     */
    long deleteByEventoId(Long eventoId);

    /**
     * Busca un asiento puntual por evento + fila + columna.
     *
     * Se usa en VentaSyncService para vincular los asientos del request de venta
     * con los asientos reales persistidos en la base.
     */
    Optional<Asiento> findByEventoIdAndFilaAndColumna(Long eventoId, Integer fila, Integer columna);

    List<Asiento> findByEventoId(Long eventoId);

}
