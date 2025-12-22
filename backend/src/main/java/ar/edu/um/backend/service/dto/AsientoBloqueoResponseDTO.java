package ar.edu.um.backend.service.dto;
import lombok.*;
import java.io.Serializable;
import java.util.List;
/**
 * Respuesta del bloqueo de asientos (Payload 6).
 *
 * Indica si el bloqueo fue exitoso y el estado final
 * de cada asiento solicitado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsientoBloqueoResponseDTO implements Serializable {

    private Long eventoId;
    private boolean resultado;
    private String descripcion;
    private List<AsientoEstadoDTO> asientos;
}
