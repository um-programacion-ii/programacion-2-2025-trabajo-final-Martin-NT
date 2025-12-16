package ar.edu.um.backend.service.dto;
import java.util.List;
/**
 * DTO que modela la respuesta completa que envía el proxy
 * para el estado de asientos de un evento.
 *
 * Estructura esperada del JSON:
 * {
 *   "eventoId": 1,
 *   "asientos": [
 *     { "fila": 1, "columna": 3, "estado": "Bloqueado", "expira": "2025-11-20T02:30:32.980225020Z" },
 *     { "fila": 2, "columna": 3, "estado": "Vendido",   "expira": null },
 *     ...
 *   ]
 * }
 *
 * - {@code eventoId} → ID del evento en la cátedra (externalId).
 * - {@code asientos} → lista de asientos bloqueados/vendidos ({@link ProxyAsientoDTO}).
 *
 * El {@link ar.edu.um.backend.service.AsientoSyncService} usa este DTO como "wrapper"
 * para deserializar el JSON del proxy y luego trabajar solo con la lista de asientos.
 */
public class ProxyEstadoAsientosResponse {
    /**
     * ID del evento en la cátedra (externalId).
     */
    private Long eventoId;
    /**
     * Lista de asientos bloqueados/vendidos para ese evento.
     */
    private List<ProxyAsientoDTO> asientos;

    public Long getEventoId() {
        return eventoId;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    public List<ProxyAsientoDTO> getAsientos() {
        return asientos;
    }

    public void setAsientos(List<ProxyAsientoDTO> asientos) {
        this.asientos = asientos;
    }
}
