package ar.edu.um.backend.service.dto;
import java.io.Serializable;
import java.util.List;
/**
 * DTO que representa la petición de venta que llega desde el frontend.
 * El backend usará esta info para:
 *  - validar bloqueos en Redis,
 *  - calcular cantidad de asientos,
 *  - construir la venta real a enviar al proxy/cátedra.
 */
public class VentaRequestDTO implements Serializable {
    private Long eventoIdLocal; // id de tu tabla evento (ej: 1001)
    private List<AsientoVentaDTO> asientos; // asientos seleccionados (fila/columna)

    public Long getEventoIdLocal() {
        return eventoIdLocal;
    }

    public void setEventoIdLocal(Long eventoIdLocal) {
        this.eventoIdLocal = eventoIdLocal;
    }

    public List<AsientoVentaDTO> getAsientos() {
        return asientos;
    }

    public void setAsientos(List<AsientoVentaDTO> asientos) {
        this.asientos = asientos;
    }

    @Override
    public String toString() {
        return "VentaRequestDTO{" +
            "eventoIdLocal=" + eventoIdLocal +
            ", asientos=" + asientos +
            '}';
    }
}
