package ar.edu.um.backend.service.dto;
import lombok.*;
import java.io.Serializable;
/**
 * Estado final de un asiento para mostrar al frontend,
 * combinando DB local + Redis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsientoEstadoDTO implements Serializable {

    private Integer fila;
    private Integer columna;
    private String estado;     // LIBRE / BLOQUEADO_VIGENTE / BLOQUEADO_EXPIRADO / VENDIDO
}
