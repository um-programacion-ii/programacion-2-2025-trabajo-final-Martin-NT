package ar.edu.um.tpfinal.viewmodel

import ar.edu.um.tpfinal.dto.AsientoUbicacionDTO
import ar.edu.um.tpfinal.dto.VentaAsientoFrontendDTO
import ar.edu.um.tpfinal.dto.VentaRequestDTO
import ar.edu.um.tpfinal.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SeatsViewModel {

    private val _uiState = MutableStateFlow<SeatsUiState>(SeatsUiState.Inactivo)
    val uiState: StateFlow<SeatsUiState> = _uiState.asStateFlow()

    // Persistimos la selección para poder vender aunque el estado cambie a Bloqueados
    private var seleccionActual: List<Pair<Int, Int>> = emptyList()

    // =========================
    // CARGAR MAPA FINAL
    // =========================
    suspend fun cargarMapa(eventoId: Long) {
        _uiState.value = SeatsUiState.CargandoAsientos

        val result = ApiClient.getAsientosEvento(eventoId.toLong())

        result.fold(
            onSuccess = { asientos ->
                seleccionActual = emptyList()
                _uiState.value = SeatsUiState.AsientosCargados(asientos)
            },
            onFailure = { error ->
                _uiState.value = SeatsUiState.Error(
                    error.message ?: "Error al cargar el mapa de asientos"
                )
            }
        )
    }

    // =========================
    // SELECCIÓN (máx. 4)
    // =========================
    fun alternarSeleccion(fila: Int, columna: Int) {
        val estadoActual = _uiState.value
        if (estadoActual !is SeatsUiState.AsientosCargados) return

        val nuevos = estadoActual.seleccionados.toMutableList()
        val asiento = fila to columna

        if (nuevos.contains(asiento)) {
            nuevos.remove(asiento)
        } else {
            if (nuevos.size >= 4) return
            nuevos.add(asiento)
        }

        // Guardamos selección actual
        seleccionActual = nuevos

        _uiState.value = estadoActual.copy(seleccionados = nuevos)
    }

    // =========================
    // BLOQUEO
    // =========================
    suspend fun bloquearSeleccionados(eventoId: Long) {
        val estadoActual = _uiState.value
        if (estadoActual !is SeatsUiState.AsientosCargados) return

        val seleccion = estadoActual.seleccionados
        if (seleccion.isEmpty()) return

        // Persistimos selección por si cambia el estado
        seleccionActual = seleccion

        _uiState.value = SeatsUiState.Bloqueando

        val asientos = seleccion.map { (fila, col) ->
            AsientoUbicacionDTO(fila, col)
        }

        val result = ApiClient.bloquearAsientos(eventoId, asientos)

        result.fold(
            onSuccess = {
                _uiState.value = SeatsUiState.Bloqueados
            },
            onFailure = { error ->
                _uiState.value = SeatsUiState.Error(
                    error.message ?: "Error al bloquear asientos"
                )
            }
        )
    }

    // =========================
    // VENTA
    // =========================
    suspend fun venderAsientos(
        eventoId: Long,
        persona: String
    ) {
        if (seleccionActual.isEmpty()) {
            _uiState.value = SeatsUiState.Error("No hay asientos seleccionados para vender")
            return
        }

        _uiState.value = SeatsUiState.Vendiendo

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

        val result = ApiClient.venderAsientos(eventoId, request)

        result.fold(
            onSuccess = { ventaResponse ->
                // Ahora se pasa el resultado (ventaResponse) a la data class
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
