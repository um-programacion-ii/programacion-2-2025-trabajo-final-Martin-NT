package ar.edu.um.proxyservice.service.dto;
import java.io.Serializable;
import java.util.List;
import lombok.*;
/**
 * Payload 6 - Request de bloqueo de asientos en un evento.
 *
 * Entrada esperada por la cátedra:
 * {
 *   "eventoId": 1,
 *   "asientos": [ {"fila":2,"columna":1}, ... ]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloquearAsientosRequestDTO implements Serializable {
    private Long eventoId;
    private List<AsientoUbicacionDTO> asientos;
}
