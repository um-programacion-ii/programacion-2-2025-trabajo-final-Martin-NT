package ar.edu.um.proxyservice.service.dto;
import java.time.Instant;
/**
 * DTO que representa un asiento tal como viene desde el Redis REMOTO de la cátedra
 * o como es enviado/recibido en los endpoints del proxy.
 *
 * Este objeto ahora incluye 'personaActual' para poder recibir el username
 * durante las llamadas de bloqueo desde el Backend.
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


    @Override
    public String toString() {
        return "AsientoRemotoDTO{" +
                "fila=" + fila +
                ", columna=" + columna +
                ", estado='" + estado + '\'' +
                ", expira=" + expira +
                '}';
    }
}