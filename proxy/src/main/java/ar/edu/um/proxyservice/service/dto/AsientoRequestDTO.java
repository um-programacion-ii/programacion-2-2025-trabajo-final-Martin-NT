package ar.edu.um.proxyservice.service.dto;
import java.io.Serializable;
import java.time.Instant;
import lombok.*;
/**
 * DTO de asiento tal como viene desde Redis REMOTO de la cátedra (vía proxy).
 *
 * Usos:
 * - Respuesta de /api/proxy/eventos/{id}/estado-asientos
 * - Respuesta de /api/proxy/eventos/{id}/asientos (si lo exponés igual)
 *
 * Campos esperados (según cátedra/redis):
 * - fila / columna
 * - estado: "Libre" | "Bloqueado" | "Ocupado" | "Vendido" (puede variar)
 * - expira: timestamp de expiración del bloqueo (puede venir null)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsientoRequestDTO implements Serializable {
    private Integer fila;
    private Integer columna;
    private String estado;
    private Instant expira; // puede ser null
}
