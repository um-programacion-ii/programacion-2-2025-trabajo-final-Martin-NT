package ar.edu.um.backend.service.dto;
import java.io.Serializable;
import java.util.List;
/**
 * DTO utilizado para iniciar una venta desde el frontend.
 *
 * Representa la intención de compra de un usuario y
 * NO se persiste en la base de datos.
 *
 * Contiene únicamente la información mínima necesaria
 * para comenzar el proceso de venta:
 *  - el ID LOCAL del evento (id de la tabla Evento),
 *  - la lista de asientos seleccionados (fila y columna).
 *
 * El backend utiliza este DTO para:
 *  - validar que los asientos existan para el evento,
 *  - validar que estén bloqueados de forma vigente (Redis),
 *  - validar que no estén vendidos,
 *  - construir la venta real que se envía al proxy/cátedra.
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
