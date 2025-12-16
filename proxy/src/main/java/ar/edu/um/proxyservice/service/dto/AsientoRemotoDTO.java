package ar.edu.um.proxyservice.service.dto;
import java.time.Instant;
/**
 * DTO que representa un asiento tal como viene desde el Redis REMOTO de la cátedra.
 * Este objeto refleja exactamente el formato externo del JSON almacenado en Redis:
 *   {
 *     "fila": 1,
 *     "columna": 2,
 *     "estado": "BLOQUEADO",
 *     "expira": "2025-11-30T10:00:00Z"
 *   }
 */
public class AsientoRemotoDTO {
    private Integer fila;
    private Integer columna;
    private String estado;
    private Instant expira;

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

    public Instant getExpira() {
        return expira;
    }

    public void setExpira(Instant expira) {
        this.expira = expira;
    }
}

