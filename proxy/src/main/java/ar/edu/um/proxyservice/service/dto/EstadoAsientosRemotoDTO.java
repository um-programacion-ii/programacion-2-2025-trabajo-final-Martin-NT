package ar.edu.um.proxyservice.service.dto;
import java.util.ArrayList;
import java.util.List;
/**
 * DTO que representa el estado completo de los asientos de un evento,
 * según el JSON almacenado en Redis por la cátedra.
 *
 * Ejemplo de JSON remoto:
 *   {
 *     "eventoId": 5,
 *     "asientos": [ ... lista de AsientoRemotoDTO ... ]
 *   }
 *
 * Su propósito principal es:
 * - transportar datos del Redis remoto hacia el backend del alumno,
 * - permitir parsear el JSON recibido,
 * - desacoplar el formato externo del modelo interno.
 */
public class EstadoAsientosRemotoDTO {
    private Long eventoId;
    private List<AsientoRemotoDTO> asientos = new ArrayList<>();

    public Long getEventoId() {
        return eventoId;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    public List<AsientoRemotoDTO> getAsientos() {
        return asientos;
    }

    public void setAsientos(List<AsientoRemotoDTO> asientos) {
        this.asientos = asientos;
    }
}
