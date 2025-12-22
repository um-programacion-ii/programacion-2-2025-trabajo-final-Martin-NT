package ar.edu.um.backend.service.dto;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
/**
 * Evento resumido devuelto por la cátedra (Payload 3).
 * Usado solo para listados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyEventoResumenDTO implements Serializable {

    private Long id;
    private String titulo;
    private String resumen;
    private String descripcion;
    private Instant fecha;
    private BigDecimal precioEntrada;
    private ProxyTipoEventoDTO eventoTipo;
}
