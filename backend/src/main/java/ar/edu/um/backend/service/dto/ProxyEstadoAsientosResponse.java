package ar.edu.um.backend.service.dto;
import lombok.*;
import java.io.Serializable;
import java.util.List;
/**
 * DTO de respuesta del proxy con el estado de asientos
 * de un evento (Redis).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyEstadoAsientosResponse implements Serializable {

    private Long eventoId; // externalId
    private List<AsientoRequestDTO> asientos;
}
