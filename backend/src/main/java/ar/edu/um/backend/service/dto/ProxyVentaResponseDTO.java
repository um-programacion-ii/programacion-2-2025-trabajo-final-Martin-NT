package ar.edu.um.backend.service.dto;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
/**
 * Respuesta de una venta individual (Payload 7 y Payload 9).
 * Devuelta por el proxy/cátedra.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyVentaResponseDTO implements Serializable {
    private Long eventoId;
    private Long ventaId;
    private Instant fechaVenta;
    private List<ProxyVentaAsientoDTO> asientos; // vacío si la venta falló
    private Boolean resultado;
    private String descripcion;
    private BigDecimal precioVenta;
}
