package ar.edu.um.tpfinal.viewmodel
import ar.edu.um.tpfinal.dto.AsientoUbicacionDTO
import ar.edu.um.tpfinal.dto.VentaAsientoFrontendDTO
import ar.edu.um.tpfinal.dto.VentaRequestDTO
import ar.edu.um.tpfinal.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
/**
 * ViewModel responsable de la pantalla de selección, bloqueo y venta de asientos.
 *
 * Rol:
 * - Solicita al backend el MAPA FINAL de asientos del evento.
 * - Gestiona la selección de asientos (máximo 4).
 * - Coordina el bloqueo de asientos (Payload 6).
 * - Coordina la venta de asientos (Payload 7).
 *
 * Importante:
 * - NO dibuja UI.
 * - NO conoce Redis ni Proxy.
 * - Toda comunicación es vía ApiClient → Backend.
 */
class SeatsViewModel {

    // UI STATE

    // Estado observable de la pantalla de asientos
    private val _uiState = MutableStateFlow<SeatsUiState>(SeatsUiState.Inactivo)
    val uiState: StateFlow<SeatsUiState> = _uiState.asStateFlow()

    // Persistimos la selección para poder vender
    // incluso si el estado de UI cambia (ej: Bloqueando / Bloqueados).
    private var seleccionActual: List<Pair<Int, Int>> = emptyList()

    /**
     * Carga el mapa FINAL de asientos del evento.
     *
     * Backend endpoint: GET /api/eventos/{externalId}/asientos
     *
     * El backend:
     * - consulta Redis vía proxy,
     * - completa LIBRES por diferencia,
     * - devuelve la grilla completa.
     */
    suspend fun cargarMapa(eventoId: Long) {

        // Estado de carga
        _uiState.value = SeatsUiState.CargandoAsientos

        // Llamada al backend
        val result = ApiClient.getAsientosEvento(eventoId.toLong())

        result.fold(
            onSuccess = { asientos ->
                // Se limpia selección previa
                seleccionActual = emptyList()

                // [UI] Asientos listos para renderizar
                _uiState.value = SeatsUiState.AsientosCargados(asientos)
            },
            onFailure = { error ->
                _uiState.value = SeatsUiState.Error(
                    error.message ?: "Error al cargar el mapa de asientos"
                )
            }
        )
    }

    /**
     * Alterna la selección de un asiento (fila, columna).
     *
     * Reglas:
     * - Solo funciona si el mapa está cargado.
     * - Máximo 4 asientos seleccionados.
     */
    fun alternarSeleccion(fila: Int, columna: Int) {

        val estadoActual = _uiState.value
        if (estadoActual !is SeatsUiState.AsientosCargados) return

        val nuevos = estadoActual.seleccionados.toMutableList()
        val asiento = fila to columna

        if (nuevos.contains(asiento)) {
            // Deseleccionar
            nuevos.remove(asiento)
        } else {
            // Máximo 4 asientos
            if (nuevos.size >= 4) return
            nuevos.add(asiento)
        }

        // [SELECCIÓN] Persistimos selección actual
        seleccionActual = nuevos

        // [UI] Se actualiza el estado con la nueva selección
        _uiState.value = estadoActual.copy(seleccionados = nuevos)
    }

    /**
     * Solicita el bloqueo de los asientos seleccionados.
     * Backend endpoint: POST /api/eventos/{externalId}/bloqueos
     */
    suspend fun bloquearSeleccionados(eventoId: Long) {

        val estadoActual = _uiState.value
        if (estadoActual !is SeatsUiState.AsientosCargados) return

        val seleccion = estadoActual.seleccionados
        if (seleccion.isEmpty()) return

        // [SELECCIÓN] Persistimos por si el estado de UI cambia
        seleccionActual = seleccion

        // [UI] Estado bloqueando
        _uiState.value = SeatsUiState.Bloqueando

        // [DTO] Mapeo a ubicaciones simples (fila/columna)
        val asientos = seleccion.map { (fila, col) ->
            AsientoUbicacionDTO(fila, col)
        }

        // [NETWORK] Llamada al backend
        val result = ApiClient.bloquearAsientos(eventoId, asientos)

        result.fold(
            onSuccess = {
                // [UI] Bloqueo confirmado
                _uiState.value = SeatsUiState.Bloqueados
            },
            onFailure = { error ->
                _uiState.value = SeatsUiState.Error(
                    error.message ?: "Error al bloquear asientos"
                )
            }
        )
    }

    /**
     * Confirma la venta de los asientos previamente bloqueados.
     *
     * Backend endpoint: POST /api/ventas/eventos/{externalId}/venta
     */
    suspend fun venderAsientos(
        eventoId: Long,
        persona: String
    ) {

        // [VALIDACIÓN] Debe haber selección persistida
        if (seleccionActual.isEmpty()) {
            _uiState.value = SeatsUiState.Error("No hay asientos seleccionados para vender")
            return
        }

        // [UI] Estado vendiendo
        _uiState.value = SeatsUiState.Vendiendo

        // [DTO] Construcción del request de venta
        val request = VentaRequestDTO(
            eventoId = eventoId,
            asientos = seleccionActual.map { (fila, col) ->
                VentaAsientoFrontendDTO(
                    fila = fila,
                    columna = col,
                    persona = persona
                )
            }
        )

        // [NETWORK] Llamada al backend
        val result = ApiClient.venderAsientos(eventoId, request)

        result.fold(
            onSuccess = { ventaResponse ->
                // [SUCCESS] Venta confirmada
                _uiState.value = SeatsUiState.Exito(venta = ventaResponse)
            },
            onFailure = { error ->
                _uiState.value = SeatsUiState.Error(
                    error.message ?: "Error al realizar la venta"
                )
            }
        )
    }
}
