package ar.edu.um.backend.service.dto;
import lombok.*;
import java.io.Serializable;
/**
 * DTO de integración que representa el tipo/categoría de un evento
 * tal como lo expone la cátedra. (P3 y P4)
 *
 * Tipos de evento (conferencia, obra de teatro, recital, etc.)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyTipoEventoDTO implements Serializable {

    // Nombre del tipo de evento (ej: "Conferencia", "Obra de teatro").
    private String nombre;

    private String descripcion;
}
