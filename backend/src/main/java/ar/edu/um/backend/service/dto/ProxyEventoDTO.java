package ar.edu.um.backend.service.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO que representa la estructura de un "Evento" tal como lo envía
 * el proxy-service (y originalmente la cátedra).
 *
 * Este objeto es el puente entre los datos remotos JSON y el modelo
 * interno del backend (la entidad Evento).
 *
 * El EventoSyncService transforma este DTO en una entidad Evento local.
 */
public class ProxyEventoDTO {
    /**
     * ID real del evento en la cátedra.
     * Este valor se guarda como "externalId" en la entidad Evento local.
     * Permite saber qué evento local corresponde a cuál evento remoto.
     */
    private Long id;
    private String titulo;
    private String descripcion;
    private LocalDate fecha;
    private LocalTime hora; // Si es null se setea 00:00.
    private String organizador;
    private String presentadores;
    private Integer cantidadAsientosTotales;
    private Integer filaAsientos;
    @JsonProperty("columnAsientos")  // Nombre EXACTO del JSON de la cátedra
    private Integer columnaAsientos;

    // GETTERS & SETTERS
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }

    public String getOrganizador() { return organizador; }
    public void setOrganizador(String organizador) { this.organizador = organizador; }

    public String getPresentadores() { return presentadores; }
    public void setPresentadores(String presentadores) { this.presentadores = presentadores; }

    public Integer getCantidadAsientosTotales() { return cantidadAsientosTotales; }
    public void setCantidadAsientosTotales(Integer cantidadAsientosTotales) {
        this.cantidadAsientosTotales = cantidadAsientosTotales;
    }

    public Integer getFilaAsientos() { return filaAsientos; }
    public void setFilaAsientos(Integer filaAsientos) { this.filaAsientos = filaAsientos; }

    public Integer getColumnaAsientos() { return columnaAsientos; }
    public void setColumnaAsientos(Integer columnaAsientos) { this.columnaAsientos = columnaAsientos; }
}
