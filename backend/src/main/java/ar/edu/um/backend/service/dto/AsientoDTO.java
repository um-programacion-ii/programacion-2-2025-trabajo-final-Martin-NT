package ar.edu.um.backend.service.dto;

import ar.edu.um.backend.domain.enumeration.AsientoEstado;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link ar.edu.um.backend.domain.Asiento} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AsientoDTO implements Serializable {

    private Long id;

    @NotNull
    private Integer fila;

    @NotNull
    private Integer columna;

    @NotNull
    private AsientoEstado estado;

    private String personaActual;

    @NotNull
    private EventoDTO evento;

    private Set<VentaDTO> ventas = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public AsientoEstado getEstado() {
        return estado;
    }

    public void setEstado(AsientoEstado estado) {
        this.estado = estado;
    }

    public String getPersonaActual() {
        return personaActual;
    }

    public void setPersonaActual(String personaActual) {
        this.personaActual = personaActual;
    }

    public EventoDTO getEvento() {
        return evento;
    }

    public void setEvento(EventoDTO evento) {
        this.evento = evento;
    }

    public Set<VentaDTO> getVentas() {
        return ventas;
    }

    public void setVentas(Set<VentaDTO> ventas) {
        this.ventas = ventas;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AsientoDTO)) {
            return false;
        }

        AsientoDTO asientoDTO = (AsientoDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, asientoDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AsientoDTO{" +
            "id=" + getId() +
            ", fila=" + getFila() +
            ", columna=" + getColumna() +
            ", estado='" + getEstado() + "'" +
            ", personaActual='" + getPersonaActual() + "'" +
            ", evento=" + getEvento() +
            ", ventas=" + getVentas() +
            "}";
    }
}
