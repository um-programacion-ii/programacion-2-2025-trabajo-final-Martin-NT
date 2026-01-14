package ar.edu.um.backend.service.dto;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
/**
 * DTO enviado al proxy/cátedra para realizar una venta (Payload 7 - request).
 * Usa el externalId del evento.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyVentaRequestDTO implements Serializable {
    private Long eventoId; // externalId
    private Instant fecha;
    private BigDecimal precioVenta;
    private List<VentaAsientoFrontendDTO> asientos;
}
