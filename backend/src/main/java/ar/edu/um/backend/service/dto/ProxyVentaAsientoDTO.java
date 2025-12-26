package ar.edu.um.backend.service.dto;
import lombok.*;
import java.io.Serializable;
/**
 * DTO que representa un asiento dentro de la respuesta de venta
 * enviada por la cátedra (Payload 7 y 9).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyVentaAsientoDTO implements Serializable {
    private Integer fila;
    private Integer columna;
    private String persona;
    private String estado; // "Vendido" / "Libre" / "Ocupado"
}

