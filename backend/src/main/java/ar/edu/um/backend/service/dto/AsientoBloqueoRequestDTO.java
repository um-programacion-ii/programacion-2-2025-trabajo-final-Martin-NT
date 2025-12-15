package ar.edu.um.backend.service.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
/**
 * DTO para solicitar el bloqueo de un asiento desde el backend del alumno.
 *
 * Se usa en:
 *   POST /api/eventos/{id}/bloqueos
 *
 * Este DTO representa el request recibido desde el frontend/Postman.
 * Luego, el backend traduce esta solicitud y la envía al proxy,
 * que es el encargado de comunicarse con el servidor de la cátedra.
 */
public class AsientoBloqueoRequestDTO implements Serializable {

    @NotNull
    @Min(1)
    private Integer fila;

    @NotNull
    @Min(1)
    private Integer columna;

    public Integer getFila() {
        return fila;
    }

    public void setFila(Integer fila) {
        this.fila = fila;
    }

    public Integer getColumna() {
        return columna;
    }

    public void setColumna(Integer columna) {
        this.columna = columna;
    }

    @Override
    public String toString() {
        return "AsientoBloqueoRequestDTO{" +
            "fila=" + fila +
            ", columna=" + columna +
            '}';
    }
}

