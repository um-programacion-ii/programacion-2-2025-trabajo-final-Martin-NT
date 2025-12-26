package ar.edu.um.backend.service.dto;
import lombok.*;
import java.io.Serializable;
/**
 * DTO de integración que representa a un integrante/participante
 * de un evento según la información provista por la cátedra.
 *
 * Los integrantes llegan como una lista de objetos compuestos
 *  dentro del evento (nombre, apellido, identificación). (P4 y P5)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyIntegranteEventoDTO implements Serializable {

    private String nombre;
    private String apellido;
    // Rol, título o identificación (ej: "Dra.", "Profesor").
    private String identificacion;
}
