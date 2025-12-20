package ar.edu.um.backend.service.dto;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
/**
 * DTO de listado resumido de ventas (Payload 8).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyVentaResumenDTO implements Serializable {
    private Long eventoId;
    private Long ventaId;
    private Instant fechaVenta;
    private Boolean resultado;
    private String descripcion;
    private BigDecimal precioVenta;
    private Integer cantidadAsientos;
}
