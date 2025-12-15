package ar.edu.um.backend.service.dto;
import java.io.Serializable;
import java.time.LocalDateTime;
/**
 * DTO devuelto al bloquear un asiento.
 *
 * Contiene la información mínima para el frontend:
 *  - fila / columna del asiento,
 *  - estado final del asiento después del bloqueo,
 *  - fecha/hora exacta en la que expira el bloqueo (ahora + 5 minutos).
 */
public class AsientoBloqueoResponseDTO implements Serializable {
    private Integer fila;
    private Integer columna;
    private String estado; // LIBRE / BLOQUEADO / VENDIDO / etc.
    private LocalDateTime expiraA; // ahora + 5 minutos

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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getExpiraA() {
        return expiraA;
    }

    public void setExpiraA(LocalDateTime expiraA) {
        this.expiraA = expiraA;
    }

    @Override
    public String toString() {
        return "AsientoBloqueoResponseDTO{" +
            "fila=" + fila +
            ", columna=" + columna +
            ", estado='" + estado + '\'' +
            ", expiraA=" + expiraA +
            '}';
    }
}
