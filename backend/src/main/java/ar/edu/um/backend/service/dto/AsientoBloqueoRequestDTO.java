package ar.edu.um.backend.service.dto;
import lombok.*;
import java.io.Serializable;
import java.util.List;
/**
 * Request para bloquear uno o más asientos de un evento (Payload 6).
 *
 * Se envía desde el backend al proxy/cátedra.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsientoBloqueoRequestDTO implements Serializable {
    private Long eventoId; // externalId
    private List<AsientoUbicacionDTO> asientos;
}
