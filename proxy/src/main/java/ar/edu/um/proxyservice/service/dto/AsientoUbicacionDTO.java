package ar.edu.um.proxyservice.service.dto;
import java.io.Serializable;
import lombok.*;
/**
 * Coordenadas de un asiento (fila/columna).
 *
 * Usos:
 * - Payload 6 (bloqueo) request: asientos[]
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsientoUbicacionDTO implements Serializable {
    private Integer fila;
    private Integer columna;
}
