package ar.edu.um.proxyservice.service.dto;
import java.io.Serializable;
import lombok.*;
/**
 * Resultado por asiento en la respuesta del bloqueo (Payload 6 salida).
 *
 * estado ejemplo:
 * - "Bloqueo exitoso"
 * - "Bloqueado"
 * - "Ocupado"
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloqueoAsientoEstadoDTO implements Serializable {
    private String estado;
    private Integer fila;
    private Integer columna;
}
