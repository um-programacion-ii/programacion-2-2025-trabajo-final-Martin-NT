package ar.edu.um.backend.domain;

import ar.edu.um.backend.domain.enumeration.VentaEstado;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Entidad JPA que representa una venta registrada en la base de datos local.
 */
@Entity
@Table(name = "venta")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Venta implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    /**
     * Identificador de la venta en el sistema remoto (cátedra),
     */
    @Column(name = "external_id")
    private Long externalId;

    @NotNull
    @Column(name = "fecha_venta", nullable = false)
    private LocalDate fechaVenta;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private VentaEstado estado;

    @Column(name = "descripcion")
    private String descripcion;

    @NotNull
    @Column(name = "precio_venta", precision = 21, scale = 2, nullable = false)
    private BigDecimal precioVenta;

    @NotNull
    @Column(name = "cantidad_asientos", nullable = false)
    private Integer cantidadAsientos;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "asientos", "ventas" }, allowSetters = true)
    private Evento evento;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rel_venta__asientos",
        joinColumns = @JoinColumn(name = "venta_id"),
        inverseJoinColumns = @JoinColumn(name = "asientos_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "evento", "ventas" }, allowSetters = true)
    private Set<Asiento> asientos = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Venta id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExternalId() {
        return externalId;
    }

    public void setExternalId(Long externalId) {
        this.externalId = externalId;
    }

    public Venta externalId(Long externalId) {
        this.externalId = externalId;
        return this;
    }

    public LocalDate getFechaVenta() {
        return this.fechaVenta;
    }

    public Venta fechaVenta(LocalDate fechaVenta) {
        this.setFechaVenta(fechaVenta);
        return this;
    }

    public void setFechaVenta(LocalDate fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public VentaEstado getEstado() {
        return this.estado;
    }

    public Venta estado(VentaEstado estado) {
        this.setEstado(estado);
        return this;
    }

    public void setEstado(VentaEstado estado) {
        this.estado = estado;
    }

    public String getDescripcion() {
        return this.descripcion;
    }

    public Venta descripcion(String descripcion) {
        this.setDescripcion(descripcion);
        return this;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecioVenta() {
        return this.precioVenta;
    }

    public Venta precioVenta(BigDecimal precioVenta) {
        this.setPrecioVenta(precioVenta);
        return this;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public Integer getCantidadAsientos() {
        return this.cantidadAsientos;
    }

    public Venta cantidadAsientos(Integer cantidadAsientos) {
        this.setCantidadAsientos(cantidadAsientos);
        return this;
    }

    public void setCantidadAsientos(Integer cantidadAsientos) {
        this.cantidadAsientos = cantidadAsientos;
    }

    public Evento getEvento() {
        return this.evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public Venta evento(Evento evento) {
        this.setEvento(evento);
        return this;
    }

    public Set<Asiento> getAsientos() {
        return this.asientos;
    }

    public void setAsientos(Set<Asiento> asientos) {
        this.asientos = asientos;
    }

    public Venta asientos(Set<Asiento> asientos) {
        this.setAsientos(asientos);
        return this;
    }

    public Venta addAsientos(Asiento asiento) {
        this.asientos.add(asiento);
        return this;
    }

    public Venta removeAsientos(Asiento asiento) {
        this.asientos.remove(asiento);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Venta)) {
            return false;
        }
        return getId() != null && getId().equals(((Venta) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Venta{" +
            "id=" + getId() +
            ", fechaVenta='" + getFechaVenta() + "'" +
            ", estado='" + getEstado() + "'" +
            ", descripcion='" + getDescripcion() + "'" +
            ", precioVenta=" + getPrecioVenta() +
            ", cantidadAsientos=" + getCantidadAsientos() +
            "}";
    }
}
