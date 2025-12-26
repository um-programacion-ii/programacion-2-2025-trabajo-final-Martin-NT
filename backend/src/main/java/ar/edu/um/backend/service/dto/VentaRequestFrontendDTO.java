package ar.edu.um.backend.service.dto;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO recibido desde el frontend para iniciar una venta.
 * Usa el ID LOCAL del evento.
 *
 * No se envía al proxy directamente.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaRequestFrontendDTO implements Serializable {
    private Long eventoIdLocal; // id local (DB)
    private BigDecimal precioVenta; // precio total
    private List<VentaAsientoFrontendDTO> asientos;
}
