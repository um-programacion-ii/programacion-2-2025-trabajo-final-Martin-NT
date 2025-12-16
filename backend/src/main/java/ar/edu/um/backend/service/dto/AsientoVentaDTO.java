package ar.edu.um.backend.service.dto;
import java.io.Serializable;
/**
 * DTO simple que representa un asiento únicamente
 * por su posición dentro del evento (fila y columna).
 *
 * No representa un asiento persistido ni su estado
 * (LIBRE, VENDIDO, BLOQUEADO, etc.).
 *
 * Se utiliza cuando solo importa identificar
 * qué asiento se selecciona para una venta.
 *
 * Este DTO se usa:
 *  - en VentaRequestDTO (datos enviados por el frontend),
 *  - en ProxyVentaDTO (datos enviados al proxy/cátedra).
 */
public class AsientoVentaDTO implements Serializable {
    private Integer fila;
    private Integer columna;

    public AsientoVentaDTO() {}

    public AsientoVentaDTO(Integer fila, Integer columna) {
        this.fila = fila;
        this.columna = columna;
    }

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
        return "AsientoVentaDTO{" +
            "fila=" + fila +
            ", columna=" + columna +
            '}';
    }
}
