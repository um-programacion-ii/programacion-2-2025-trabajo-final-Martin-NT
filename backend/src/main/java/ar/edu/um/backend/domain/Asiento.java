package ar.edu.um.backend.domain;

import ar.edu.um.backend.domain.enumeration.AsientoEstado;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Asiento.
 */
@Entity
@Table(name = "asiento")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Asiento implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "fila", nullable = false)
    private Integer fila;

    @NotNull
    @Column(name = "columna", nullable = false)
    private Integer columna;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private AsientoEstado estado;

    @Column(name = "persona_actual")
    private String personaActual;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "asientos", "ventas" }, allowSetters = true)
    private Evento evento;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "asientos")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "evento", "asientos" }, allowSetters = true)
    private Set<Venta> ventas = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Asiento id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getFila() {
        return this.fila;
    }

    public Asiento fila(Integer fila) {
        this.setFila(fila);
        return this;
    }

    public void setFila(Integer fila) {
        this.fila = fila;
    }

    public Integer getColumna() {
        return this.columna;
    }

    public Asiento columna(Integer columna) {
        this.setColumna(columna);
        return this;
    }

    public void setColumna(Integer columna) {
        this.columna = columna;
    }

    public AsientoEstado getEstado() {
        return this.estado;
    }

    public Asiento estado(AsientoEstado estado) {
        this.setEstado(estado);
        return this;
    }

    public void setEstado(AsientoEstado estado) {
        this.estado = estado;
    }

    public String getPersonaActual() {
        return this.personaActual;
    }

    public Asiento personaActual(String personaActual) {
        this.setPersonaActual(personaActual);
        return this;
    }

    public void setPersonaActual(String personaActual) {
        this.personaActual = personaActual;
    }

    public Evento getEvento() {
        return this.evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public Asiento evento(Evento evento) {
        this.setEvento(evento);
        return this;
    }

    public Set<Venta> getVentas() {
        return this.ventas;
    }

    public void setVentas(Set<Venta> ventas) {
        if (this.ventas != null) {
            this.ventas.forEach(i -> i.removeAsientos(this));
        }
        if (ventas != null) {
            ventas.forEach(i -> i.addAsientos(this));
        }
        this.ventas = ventas;
    }

    public Asiento ventas(Set<Venta> ventas) {
        this.setVentas(ventas);
        return this;
    }

    public Asiento addVentas(Venta venta) {
        this.ventas.add(venta);
        venta.getAsientos().add(this);
        return this;
    }

    public Asiento removeVentas(Venta venta) {
        this.ventas.remove(venta);
        venta.getAsientos().remove(this);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Asiento)) {
            return false;
        }
        return getId() != null && getId().equals(((Asiento) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Asiento{" +
            "id=" + getId() +
            ", fila=" + getFila() +
            ", columna=" + getColumna() +
            ", estado='" + getEstado() + "'" +
            ", personaActual='" + getPersonaActual() + "'" +
            "}";
    }
}
