/**
 * DTO de integración utilizado para enviar una venta desde el backend
 * al proxy-service (y posteriormente a la cátedra).
 *
 * Representa exactamente el JSON esperado por la cátedra para registrar una venta.
 *
 * Contiene:
 *  - el ID EXTERNO del evento en la cátedra,
 *  - la lista de asientos a vender (fila / columna),
 *  - el precio total de la venta.
 *
 * Este DTO NO se expone al frontend ni se persiste en la base local.
 */
package ar.edu.um.backend.service.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO de integración que representa las VENTAS tal como las expone
 * la cátedra vía proxy (payloads P7 y P8).
 *
 * Se corresponde con JSON como:
 *
 * P7 (venta individual):
 * {
 *   "eventoId": 1,
 *   "ventaId": 1501,
 *   "fechaVenta": "2025-12-12T15:49:54.700516753Z",
 *   "asientos": [ { ... } ],
 *   "resultado": true/false,
 *   "descripcion": "....",
 *   "precioVenta": 1400.0
 * }
 *
 * P8 (listado):
 * [
 *   {
 *     "eventoId": 1,
 *     "ventaId": 1501,
 *     "fechaVenta": "2025-11-27T20:15:25.135462Z",
 *     "resultado": false,
 *     "descripcion": "...",
 *     "precioVenta": 1400.1,
 *     "cantidadAsientos": 0
 *   },
 *   ...
 * ]
 *
 * Este DTO:
 *  - NO se expone al frontend,
 *  - NO se persiste directamente,
 *  - se usa sólo para parsear/entender la respuesta del proxy/cátedra.
 */
public class ProxyVentaDTO implements Serializable {

    private Long eventoId;
    private Long ventaId;
    private Instant fechaVenta;
    private List<ProxyVentaAsientoDTO> asientos;
    private Boolean resultado;
    private String descripcion;
    private BigDecimal precioVenta;
    private Integer cantidadAsientos;

    public Long getEventoId() {
        return eventoId;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    public Long getVentaId() {
        return ventaId;
    }

    public void setVentaId(Long ventaId) {
        this.ventaId = ventaId;
    }

    public Instant getFechaVenta() {
        return fechaVenta;
    }

    public void setFechaVenta(Instant fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public List<ProxyVentaAsientoDTO> getAsientos() {
        return asientos;
    }

    public void setAsientos(List<ProxyVentaAsientoDTO> asientos) {
        this.asientos = asientos;
    }

    public Boolean getResultado() {
        return resultado;
    }

    public void setResultado(Boolean resultado) {
        this.resultado = resultado;
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

    @Override
    public String toString() {
        return "ProxyVentaDTO{" +
            "eventoId=" + eventoId +
            ", ventaId=" + ventaId +
            ", fechaVenta=" + fechaVenta +
            ", asientos=" + asientos +
            ", resultado=" + resultado +
            ", descripcion='" + descripcion + '\'' +
            ", precioVenta=" + precioVenta +
            ", cantidadAsientos=" + cantidadAsientos +
            '}';
    }
}
