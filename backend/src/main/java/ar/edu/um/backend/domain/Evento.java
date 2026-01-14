package ar.edu.um.backend.domain;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Entidad JPA que representa un Evento almacenado en la base de datos local.
 *
 * Este modelo es sincronizado con la información real proveniente de la cátedra
 * a través del proxy-service. El campo `externalId` permite relacionar cada evento
 * local con su equivalente en el sistema remoto.
 */
@Entity
@Table(name = "evento")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Evento implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * ID autogenerado por la base de datos local.
     * Es la clave primaria de este evento dentro del sistema del alumno.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    /**
     * ID del evento en el sistema de la cátedra.
     * Permite hacer sincronización y saber qué evento local corresponde a cuál remoto.
     */
    @Column(name = "external_id", unique = true)
    private Long externalId;

    @NotNull
    @Column(name = "activo", nullable = false)
    private Boolean activo = true; // todos los eventos nuevos están activos

    @NotNull
    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "descripcion")
    private String descripcion;

    @NotNull
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    /**
     * Hora del evento. Obligatoria pero en la cátedra puede faltar.
     * Si falta en el proxy → se rellena con 00:00 durante la sincronización.
     */
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

    /**
     * precision = 21
     * Significa que que el número puede tener hasta 21 dígitos en total
     * (sumando los que están antes y después de la coma).
     *
     * scale = 2
     * Significa de esos dígitos, 2 deben estar después de la coma decimal.
     */
    @NotNull
    @Column(name = "precio_entrada", precision = 21, scale = 2, nullable = false)
    private BigDecimal precioEntrada;


    /**
     * RELACIÓN: Evento 1 —— N Asientos
     * Un evento puede tener MUCHOS asientos.
     * Cada asiento pertenece a UN SOLO evento.
     *
     * mappedBy = "evento" → la entidad Asiento tiene un campo `evento`
     * que es la clave foránea (FK) en la base de datos.
     *
     * @JsonIgnoreProperties evita recursión infinita
     * cuando se serializa un Evento → Asientos → Evento → Asientos...
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "evento")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "evento", "ventas" }, allowSetters = true)
    private Set<Asiento> asientos = new HashSet<>();

    /**
     * RELACIÓN: Evento 1 —— N Ventas
     * Un evento puede tener MUCHAS ventas.
     * Cada venta pertenece a UN SOLO evento.
     *
     * mappedBy = "evento" → la entidad Venta contiene el campo `evento`
     * que referencia la clave primaria del evento.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "evento")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "evento", "asientos" }, allowSetters = true)
    private Set<Venta> ventas = new HashSet<>();

    // GETTERS & SETTERS GENERADOS POR JHIPSTER
    public Boolean getActivo() {
        return activo;
    }
    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
    public Evento activo(Boolean activo) {
        this.activo = activo;
        return this;
    }

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public Evento id(Long id) { this.id = id; return this; }

    public String getTitulo() { return this.titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public Evento titulo(String titulo) { this.titulo = titulo; return this; }

    public String getDescripcion() { return this.descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Evento descripcion(String descripcion) { this.descripcion = descripcion; return this; }

    public LocalDate getFecha() { return this.fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public Evento fecha(LocalDate fecha) { this.fecha = fecha; return this; }

    public LocalTime getHora() { return this.hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }
    public Evento hora(LocalTime hora) { this.hora = hora; return this; }

    public String getOrganizador() { return this.organizador; }
    public void setOrganizador(String organizador) { this.organizador = organizador; }
    public Evento organizador(String organizador) { this.organizador = organizador; return this; }

    public String getPresentadores() { return this.presentadores; }
    public void setPresentadores(String presentadores) { this.presentadores = presentadores; }
    public Evento presentadores(String presentadores) { this.presentadores = presentadores; return this; }

    public Integer getCantidadAsientosTotales() { return this.cantidadAsientosTotales; }
    public void setCantidadAsientosTotales(Integer cantidadAsientosTotales) { this.cantidadAsientosTotales = cantidadAsientosTotales; }
    public Evento cantidadAsientosTotales(Integer cantidadAsientosTotales) { this.cantidadAsientosTotales = cantidadAsientosTotales; return this; }

    public Integer getFilaAsientos() { return this.filaAsientos; }
    public void setFilaAsientos(Integer filaAsientos) { this.filaAsientos = filaAsientos; }
    public Evento filaAsientos(Integer filaAsientos) { this.filaAsientos = filaAsientos; return this; }

    public Integer getColumnaAsientos() { return this.columnaAsientos; }
    public void setColumnaAsientos(Integer columnaAsientos) { this.columnaAsientos = columnaAsientos; }
    public Evento columnaAsientos(Integer columnaAsientos) { this.columnaAsientos = columnaAsientos; return this; }

    public Set<Asiento> getAsientos() { return this.asientos; }
    public void setAsientos(Set<Asiento> asientos) {
        if (this.asientos != null) {
            this.asientos.forEach(a -> a.setEvento(null));
        }
        if (asientos != null) {
            asientos.forEach(a -> a.setEvento(this));
        }
        this.asientos = asientos;
    }

    public Long getExternalId() { return externalId; }
    public void setExternalId(Long externalId) { this.externalId = externalId; }

    public BigDecimal getPrecioEntrada() {
        return this.precioEntrada;
    }

    public void setPrecioEntrada(BigDecimal precioEntrada) {
        this.precioEntrada = precioEntrada;
    }

    public Evento precioEntrada(BigDecimal precioEntrada) {
        this.precioEntrada = precioEntrada;
        return this;
    }

    public Evento asientos(Set<Asiento> asientos) { this.setAsientos(asientos); return this; }
    public Evento addAsientos(Asiento asiento) { this.asientos.add(asiento); asiento.setEvento(this); return this; }
    public Evento removeAsientos(Asiento asiento) { this.asientos.remove(asiento); asiento.setEvento(null); return this; }

    public Set<Venta> getVentas() { return this.ventas; }
    public void setVentas(Set<Venta> ventas) {
        if (this.ventas != null) this.ventas.forEach(v -> v.setEvento(null));
        if (ventas != null) ventas.forEach(v -> v.setEvento(this));
        this.ventas = ventas;
    }
    public Evento ventas(Set<Venta> ventas) { this.setVentas(ventas); return this; }
    public Evento addVentas(Venta venta) { this.ventas.add(venta); venta.setEvento(this); return this; }
    public Evento removeVentas(Venta venta) { this.ventas.remove(venta); venta.setEvento(null); return this; }
    // MÉTODOS ESPECIALES

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Evento)) return false;
        return getId() != null && getId().equals(((Evento) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Evento{" +
            "id=" + id +
            "IdCatedra=" + externalId +
            ", titulo='" + titulo + '\'' +
            ", descripcion='" + descripcion + '\'' +
            ", fecha=" + fecha +
            ", hora=" + hora +
            ", organizador='" + organizador + '\'' +
            ", presentadores='" + presentadores + '\'' +
            ", cantidadAsientosTotales=" + cantidadAsientosTotales +
            ", filaAsientos=" + filaAsientos +
            ", columnaAsientos=" + columnaAsientos +
            ", precioEntrada=" + precioEntrada +
            ", activo=" + getActivo() +
            "}";
    }

}
