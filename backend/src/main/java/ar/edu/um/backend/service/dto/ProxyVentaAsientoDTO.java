package ar.edu.um.backend.service.dto;
import java.io.Serializable;

/**
 * DTO que representa un asiento dentro de la respuesta de venta
 * que envía la cátedra (payloads tipo P7).
 *
 * Se corresponde con objetos como:
 * {
 *   "fila": 2,
 *   "columna": 3,
 *   "persona": "Fernando Galvez",
 *   "estado": "Ocupado"
 * }
 *
 * Este DTO se usa SOLO para integrar con el proxy/cátedra.
 * No se expone al frontend ni se persiste en la base local.
 */
public class ProxyVentaAsientoDTO implements Serializable {

    private Integer fila;
    private Integer columna;
    private String persona;
    private String estado;

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

    public String getPersona() {
        return persona;
    }

    public void setPersona(String persona) {
        this.persona = persona;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "ProxyVentaAsientoDTO{" +
            "fila=" + fila +
            ", columna=" + columna +
            ", persona='" + persona + '\'' +
            ", estado='" + estado + '\'' +
            '}';
    }
}

