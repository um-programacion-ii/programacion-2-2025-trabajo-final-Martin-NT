package ar.edu.um.backend.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * A DTO for the {@link ar.edu.um.backend.domain.Evento} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EventoDTO implements Serializable {

    private Long id;

    @NotNull
    private String titulo;

    private String descripcion;

    @NotNull
    private LocalDate fecha;

    @NotNull
    private LocalTime hora;

    private String organizador;

    private String presentadores;

    @NotNull
    private Integer cantidadAsientosTotales;

    @NotNull
    private Integer filaAsientos;

    @NotNull
    private Integer columnaAsientos;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public String getOrganizador() {
        return organizador;
    }

    public void setOrganizador(String organizador) {
        this.organizador = organizador;
    }

    public String getPresentadores() {
        return presentadores;
    }

    public void setPresentadores(String presentadores) {
        this.presentadores = presentadores;
    }

    public Integer getCantidadAsientosTotales() {
        return cantidadAsientosTotales;
    }

    public void setCantidadAsientosTotales(Integer cantidadAsientosTotales) {
        this.cantidadAsientosTotales = cantidadAsientosTotales;
    }

    public Integer getFilaAsientos() {
        return filaAsientos;
    }

    public void setFilaAsientos(Integer filaAsientos) {
        this.filaAsientos = filaAsientos;
    }

    public Integer getColumnaAsientos() {
        return columnaAsientos;
    }

    public void setColumnaAsientos(Integer columnaAsientos) {
        this.columnaAsientos = columnaAsientos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EventoDTO)) {
            return false;
        }

        EventoDTO eventoDTO = (EventoDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, eventoDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EventoDTO{" +
            "id=" + getId() +
            ", titulo='" + getTitulo() + "'" +
            ", descripcion='" + getDescripcion() + "'" +
            ", fecha='" + getFecha() + "'" +
            ", hora='" + getHora() + "'" +
            ", organizador='" + getOrganizador() + "'" +
            ", presentadores='" + getPresentadores() + "'" +
            ", cantidadAsientosTotales=" + getCantidadAsientosTotales() +
            ", filaAsientos=" + getFilaAsientos() +
            ", columnaAsientos=" + getColumnaAsientos() +
            "}";
    }
}
