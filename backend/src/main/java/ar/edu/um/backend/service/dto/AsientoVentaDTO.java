package ar.edu.um.backend.service.dto;
import java.io.Serializable;
/**
 * DTO simple para representar un asiento (fila/columna) involucrado en una venta.
 * Se usa tanto en VentaRequestDTO como en ProxyVentaDTO.
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
