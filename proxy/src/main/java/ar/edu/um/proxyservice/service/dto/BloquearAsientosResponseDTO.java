package ar.edu.um.proxyservice.service.dto;
import java.io.Serializable;
import java.util.List;
import lombok.*;
/**
 * Payload 6 - Respuesta de bloqueo de asientos.
 *
 * Salida esperada:
 * {
 *   "resultado": true|false,
 *   "descripcion": "...",
 *   "eventoId": 1,
 *   "asientos": [ {estado,fila,columna}, ... ]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloquearAsientosResponseDTO implements Serializable {
    private Boolean resultado;
    private String descripcion;
    private Long eventoId;
    private List<BloqueoAsientoEstadoDTO> asientos;
}
