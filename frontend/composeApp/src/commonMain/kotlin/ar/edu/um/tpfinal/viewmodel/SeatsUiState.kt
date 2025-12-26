package ar.edu.um.tpfinal.viewmodel
import ar.edu.um.tpfinal.dto.AsientoEstadoDTO
import ar.edu.um.tpfinal.dto.VentaResponseDTO
/**
 * Representa el ESTADO DE UI de la pantalla de asientos.
 *
 * Esta sealed class modela explícitamente todo el flujo:
 * - Carga del mapa final de asientos
 * - Selección
 * - Bloqueo
 * - Venta
 * - Error
 *
 * Es producida por SeatsViewModel y observada por SeatsScreen.
 */
sealed class SeatsUiState {
    object Inactivo : SeatsUiState() // Estado inicial.


    // Se está solicitando el mapa final de asientos al backend.
    object CargandoAsientos : SeatsUiState()

    /**
     * [ASIENTOS_CARGADOS]
     * El mapa final de asientos fue cargado correctamente.
     *
     * @param asientos    Grilla completa de asientos con su estado actual
     *                    (LIBRE / BLOQUEADO / VENDIDO, etc.).
     * @param seleccionados Lista de asientos seleccionados por el usuario
     *                      (fila, columna).
     *
     * Este estado es el “estado normal” de la pantalla.
     */
    data class AsientosCargados(
        val asientos: List<AsientoEstadoDTO>,
        val seleccionados: List<Pair<Int, Int>> = emptyList()
    ) : SeatsUiState()

    // Se está solicitando al backend el bloqueo de los asientos seleccionados.
    object Bloqueando : SeatsUiState()

    // El backend confirmó el bloqueo de los asientos.
    object Bloqueados : SeatsUiState()

    // Se está confirmando la venta de los asientos bloqueados.
    object Vendiendo : SeatsUiState()

    /**
     * La venta fue confirmada correctamente por el backend.
     *
     * @param venta DTO de respuesta de venta devuelto por el backend
     *    (Payload 7).
     *
     * Este estado permite:
     * - Mostrar resumen de la venta.
     * - Navegar a pantalla de confirmación.
     */
    data class Exito(
        val venta: VentaResponseDTO
    ) : SeatsUiState()

    // Se produjo un error durante el proceso.
    data class Error(
        val mensaje: String
    ) : SeatsUiState()
}
