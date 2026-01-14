package ar.edu.um.backend.service.dto;

import java.io.Serializable;
import java.util.List;

/**
 * UserSessionDTO
 * Almacena el estado de la sesión del usuario.
 */
public class UserSessionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    // Indica en que etapa del flujo esta Ej: "SELECCION_ASIENTOS", "PAGO"
    private String pasoActual;

    private Long idEventoSeleccionado;

    private List<String> asientosSeleccionados; // Ej: ["F1-C1", "F1-C2"]

    public UserSessionDTO() {
    }

    public String getPasoActual() {
        return pasoActual;
    }

    public void setPasoActual(String pasoActual) {
        this.pasoActual = pasoActual;
    }

    public Long getIdEventoSeleccionado() {
        return idEventoSeleccionado;
    }

    public void setIdEventoSeleccionado(Long idEventoSeleccionado) {
        this.idEventoSeleccionado = idEventoSeleccionado;
    }

    public List<String> getAsientosSeleccionados() {
        return asientosSeleccionados;
    }

    public void setAsientosSeleccionados(List<String> asientosSeleccionados) {
        this.asientosSeleccionados = asientosSeleccionados;
    }

    @Override
    public String toString() {
        return "UserSessionDTO{" +
            "pasoActual='" + pasoActual + '\'' +
            ", idEventoSeleccionado=" + idEventoSeleccionado +
            ", asientosSeleccionados=" + asientosSeleccionados +
            '}';
    }
}
