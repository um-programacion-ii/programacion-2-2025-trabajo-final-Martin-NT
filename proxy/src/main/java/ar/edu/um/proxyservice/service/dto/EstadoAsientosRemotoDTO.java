package ar.edu.um.proxyservice.service.dto;
import java.io.Serializable;
import java.util.List;
import lombok.*;
/**
 * DTO que representa el estado completo de asientos de un evento tal como lo guarda/devuelve la cátedra.
 *
 * Forma típica:
 * {
 *   "eventoId": 5,
 *   "asientos": [ {fila, columna, estado, expira}, ... ]
 * }
 *
 * Usos:
 * - Respuesta del proxy hacia el backend del alumno.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoAsientosRemotoDTO implements Serializable {
    private Long eventoId;
    private List<AsientoRequestDTO> asientos;
}
