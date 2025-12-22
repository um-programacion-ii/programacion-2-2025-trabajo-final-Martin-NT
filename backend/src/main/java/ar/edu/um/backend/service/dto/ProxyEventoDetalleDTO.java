package ar.edu.um.backend.service.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
/**
 * Evento completo devuelto por la cátedra (Payloads 4 y 5).
 * Usado para sincronización y detalle.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyEventoDetalleDTO implements Serializable {

    private Long id;
    private String titulo;
    private String resumen;
    private String descripcion;
    private Instant fecha;
    private String direccion;
    private String imagen;

    private Integer filaAsientos;

    @JsonProperty("columnAsientos")
    private Integer columnaAsientos;

    private BigDecimal precioEntrada;

    private ProxyTipoEventoDTO eventoTipo;
    private List<ProxyIntegranteEventoDTO> integrantes;
}
