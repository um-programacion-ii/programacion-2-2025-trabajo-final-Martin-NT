package ar.edu.um.backend.service.dto;
import lombok.*;
import java.io.Serializable;
import java.time.Instant;
/**
 * DTO que representa un asiento tal como lo envía el proxy/cátedra.
 *
 * Uso exclusivo de integración (no frontend).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsientoRequestDTO implements Serializable {

    private Integer fila;
    private Integer columna;
    private String personaActual;
    private String estado;  // LIBRE / BLOQUEADO / VENDIDO
    private Instant expira;
}
