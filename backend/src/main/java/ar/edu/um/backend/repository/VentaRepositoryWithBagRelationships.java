package ar.edu.um.backend.repository;

import ar.edu.um.backend.domain.Venta;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface VentaRepositoryWithBagRelationships {
    Optional<Venta> fetchBagRelationships(Optional<Venta> venta);

    List<Venta> fetchBagRelationships(List<Venta> ventas);

    Page<Venta> fetchBagRelationships(Page<Venta> ventas);
}
