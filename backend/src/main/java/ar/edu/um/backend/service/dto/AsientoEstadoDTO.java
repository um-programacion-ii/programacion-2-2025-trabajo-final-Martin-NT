package ar.edu.um.backend.service.dto;
import java.time.Instant;

/**
 * DTO final entregado al frontend luego de combinar:
 * - estado persistido en base local,
 * - estado temporal desde Redis.
 */
public class AsientoEstadoDTO {
    private Integer fila;
    private Integer columna;
    private String estado; // LIBRE / BLOQUEADO_VIGENTE / BLOQUEADO_EXPIRADO / VENDIDO
    private Instant expiraEn;

    public AsientoEstadoDTO() {}

    public AsientoEstadoDTO(Integer fila, Integer columna, String estado, Instant expiraEn) {
        this.fila = fila;
        this.columna = columna;
        this.estado = estado;
        this.expiraEn = expiraEn;
    }

    public Integer getFila() { return fila; }
    public void setFila(Integer fila) { this.fila = fila; }

    public Integer getColumna() { return columna; }
    public void setColumna(Integer columna) { this.columna = columna; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Instant getExpiraEn() { return expiraEn; }
    public void setExpiraEn(Instant expiraEn) { this.expiraEn = expiraEn; }
}
