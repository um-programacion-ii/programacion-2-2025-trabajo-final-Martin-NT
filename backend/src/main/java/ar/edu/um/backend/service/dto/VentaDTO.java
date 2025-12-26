package ar.edu.um.backend.service.dto;

import ar.edu.um.backend.domain.enumeration.VentaEstado;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link ar.edu.um.backend.domain.Venta} entity.
 * DTO que representa una venta expuesta por el backend.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class VentaDTO implements Serializable {

    private Long id;

    @NotNull
    private LocalDate fechaVenta;

    @NotNull
    private VentaEstado estado;

    private String descripcion;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal precioVenta;

    @NotNull
    @Min(1)
    private Integer cantidadAsientos;

    @NotNull
    private EventoDTO evento;

    private Set<AsientoDTO> asientos = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFechaVenta() {
        return fechaVenta;
    }

    public void setFechaVenta(LocalDate fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public VentaEstado getEstado() {
        return estado;
    }

    public void setEstado(VentaEstado estado) {
        this.estado = estado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public Integer getCantidadAsientos() {
        return cantidadAsientos;
    }

    public void setCantidadAsientos(Integer cantidadAsientos) {
        this.cantidadAsientos = cantidadAsientos;
    }

    public EventoDTO getEvento() {
        return evento;
    }

    public void setEvento(EventoDTO evento) {
        this.evento = evento;
    }

    public Set<AsientoDTO> getAsientos() {
        return asientos;
    }

    public void setAsientos(Set<AsientoDTO> asientos) {
        this.asientos = asientos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VentaDTO)) {
            return false;
        }

        VentaDTO ventaDTO = (VentaDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, ventaDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "VentaDTO{" +
            "id=" + getId() +
            ", fechaVenta='" + getFechaVenta() + "'" +
            ", estado='" + getEstado() + "'" +
            ", descripcion='" + getDescripcion() + "'" +
            ", precioVenta=" + getPrecioVenta() +
            ", cantidadAsientos=" + getCantidadAsientos() +
            ", evento=" + getEvento() +
            ", asientos=" + getAsientos() +
            "}";
    }
}
