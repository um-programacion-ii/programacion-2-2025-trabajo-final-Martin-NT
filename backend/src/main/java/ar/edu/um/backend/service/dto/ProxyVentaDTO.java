package ar.edu.um.backend.service.dto;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
/**
 * DTO que representa la venta que el backend envía al proxy/cátedra.
 * Normalmente incluye:
 *  - id real del evento en la cátedra (externalId),
 *  - lista de asientos a vender,
 *  - precio total opcional o calculado.
 */
public class ProxyVentaDTO implements Serializable {
    private Long eventoId; // externalId del evento en la cátedra
    private List<AsientoVentaDTO> asientos;
    private BigDecimal precioTotal; // opcional

    public Long getEventoId() {
        return eventoId;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    public List<AsientoVentaDTO> getAsientos() {
        return asientos;
    }

    public void setAsientos(List<AsientoVentaDTO> asientos) {
        this.asientos = asientos;
    }

    public BigDecimal getPrecioTotal() {
        return precioTotal;
    }

    public void setPrecioTotal(BigDecimal precioTotal) {
        this.precioTotal = precioTotal;
    }

    @Override
    public String toString() {
        return "ProxyVentaDTO{" +
            "eventoId=" + eventoId +
            ", asientos=" + asientos +
            ", precioTotal=" + precioTotal +
            '}';
    }
}
