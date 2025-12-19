package ar.edu.um.backend.service.dto;
import java.time.Instant;
/**
 * DTO que representa la estructura de un "Asiento" tal como lo envía
 * el proxy-service (y originalmente la cátedra).
 *
 * Es el puente entre los datos remotos JSON y el modelo interno del backend
 * (la entidad {@link ar.edu.um.backend.domain.Asiento}).
 *
 * El {@link ar.edu.um.backend.service.AsientoSyncService} transforma este DTO
 * en una entidad Asiento local.
 */
public class ProxyAsientoDTO {
    private Integer fila;
    private Integer columna;
    private String personaActual;
    /**
     * Estado remoto del asiento:
     * - "LIBRE"
     * - "BLOQUEADO"
     * - "VENDIDO" (u "OCUPADO" en Redis, que mapeamos a VENDIDO localmente).
     */
    private String estado;
    /**
     * Fecha/hora de expiración del bloqueo (solo para asientos BLOQUEADOS).
     * Puede ser null para asientos vendidos o libres.
     */
    private Instant expira;

    // Getters / Setters
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

    public String getPersonaActual() {
        return personaActual;
    }

    public void setPersonaActual(String personaActual) {
        this.personaActual = personaActual;
    }

    public Instant getExpira() {
        return expira;
    }

    public void setExpira(Instant expira) {
        this.expira = expira;
    }

    @Override
    public String toString() {
        return "ProxyAsientoDTO{" +
            "fila=" + fila +
            ", columna=" + columna +
            ", estado='" + estado + '\'' +
            ", personaActual='" + personaActual + '\'' +
            ", expira='" + expira + '\'' +
            '}';
    }
}
