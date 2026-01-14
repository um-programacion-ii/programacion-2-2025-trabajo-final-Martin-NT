package ar.edu.um.backend.service.dto;
import lombok.*;
import java.io.Serializable;
/**
 * DTO simple que representa la ubicación de un asiento
 * dentro de un evento (fila y columna).
 *
 * Se usa en requests (bloqueo / venta).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsientoUbicacionDTO implements Serializable {
    private Integer fila;
    private Integer columna;
}
