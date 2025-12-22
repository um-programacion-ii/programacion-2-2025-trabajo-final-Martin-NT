package ar.edu.um.backend.service.dto;
import lombok.*;
import java.io.Serializable;
/**
 * Asiento incluido en la venta desde el frontend.
 * Incluye persona (requerido por Payload 7).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaAsientoFrontendDTO implements Serializable {
    private Integer fila;
    private Integer columna;
    private String persona;
}
