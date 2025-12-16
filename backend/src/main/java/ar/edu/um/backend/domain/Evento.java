package ar.edu.um.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Evento.
 */
@Entity
@Table(name = "evento")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Evento implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "descripcion")
    private String descripcion;

    @NotNull
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @NotNull
    @Column(name = "hora", nullable = false)
    private LocalTime hora;

    @Column(name = "organizador")
    private String organizador;

    @Column(name = "presentadores")
    private String presentadores;

    @NotNull
    @Column(name = "cantidad_asientos_totales", nullable = false)
    private Integer cantidadAsientosTotales;

    @NotNull
    @Column(name = "fila_asientos", nullable = false)
    private Integer filaAsientos;

    @NotNull
    @Column(name = "columna_asientos", nullable = false)
    private Integer columnaAsientos;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "evento")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "evento", "ventas" }, allowSetters = true)
    private Set<Asiento> asientos = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "evento")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "evento", "asientos" }, allowSetters = true)
    private Set<Venta> ventas = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Evento id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return this.titulo;
    }

    public Evento titulo(String titulo) {
        this.setTitulo(titulo);
        return this;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return this.descripcion;
    }

    public Evento descripcion(String descripcion) {
        this.setDescripcion(descripcion);
        return this;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDate getFecha() {
        return this.fecha;
    }

    public Evento fecha(LocalDate fecha) {
        this.setFecha(fecha);
        return this;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return this.hora;
    }

    public Evento hora(LocalTime hora) {
        this.setHora(hora);
        return this;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public String getOrganizador() {
        return this.organizador;
    }

    public Evento organizador(String organizador) {
        this.setOrganizador(organizador);
        return this;
    }

    public void setOrganizador(String organizador) {
        this.organizador = organizador;
    }

    public String getPresentadores() {
        return this.presentadores;
    }

    public Evento presentadores(String presentadores) {
        this.setPresentadores(presentadores);
        return this;
    }

    public void setPresentadores(String presentadores) {
        this.presentadores = presentadores;
    }

    public Integer getCantidadAsientosTotales() {
        return this.cantidadAsientosTotales;
    }

    public Evento cantidadAsientosTotales(Integer cantidadAsientosTotales) {
        this.setCantidadAsientosTotales(cantidadAsientosTotales);
        return this;
    }

    public void setCantidadAsientosTotales(Integer cantidadAsientosTotales) {
        this.cantidadAsientosTotales = cantidadAsientosTotales;
    }

    public Integer getFilaAsientos() {
        return this.filaAsientos;
    }

    public Evento filaAsientos(Integer filaAsientos) {
        this.setFilaAsientos(filaAsientos);
        return this;
    }

    public void setFilaAsientos(Integer filaAsientos) {
        this.filaAsientos = filaAsientos;
    }

    public Integer getColumnaAsientos() {
        return this.columnaAsientos;
    }

    public Evento columnaAsientos(Integer columnaAsientos) {
        this.setColumnaAsientos(columnaAsientos);
        return this;
    }

    public void setColumnaAsientos(Integer columnaAsientos) {
        this.columnaAsientos = columnaAsientos;
    }

    public Set<Asiento> getAsientos() {
        return this.asientos;
    }

    public void setAsientos(Set<Asiento> asientos) {
        if (this.asientos != null) {
            this.asientos.forEach(i -> i.setEvento(null));
        }
        if (asientos != null) {
            asientos.forEach(i -> i.setEvento(this));
        }
        this.asientos = asientos;
    }

    public Evento asientos(Set<Asiento> asientos) {
        this.setAsientos(asientos);
        return this;
    }

    public Evento addAsientos(Asiento asiento) {
        this.asientos.add(asiento);
        asiento.setEvento(this);
        return this;
    }

    public Evento removeAsientos(Asiento asiento) {
        this.asientos.remove(asiento);
        asiento.setEvento(null);
        return this;
    }

    public Set<Venta> getVentas() {
        return this.ventas;
    }

    public void setVentas(Set<Venta> ventas) {
        if (this.ventas != null) {
            this.ventas.forEach(i -> i.setEvento(null));
        }
        if (ventas != null) {
            ventas.forEach(i -> i.setEvento(this));
        }
        this.ventas = ventas;
    }

    public Evento ventas(Set<Venta> ventas) {
        this.setVentas(ventas);
        return this;
    }

    public Evento addVentas(Venta venta) {
        this.ventas.add(venta);
        venta.setEvento(this);
        return this;
    }

    public Evento removeVentas(Venta venta) {
        this.ventas.remove(venta);
        venta.setEvento(null);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Evento)) {
            return false;
        }
        return getId() != null && getId().equals(((Evento) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Evento{" +
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
