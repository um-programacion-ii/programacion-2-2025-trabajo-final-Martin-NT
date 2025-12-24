package ar.edu.um.tpfinal.viewmodel

import ar.edu.um.tpfinal.dto.AsientoEstadoDTO

sealed class SeatsUiState {

    object Inactivo : SeatsUiState()

    object CargandoAsientos : SeatsUiState()

    data class AsientosCargados(
        val asientos: List<AsientoEstadoDTO>,
        val seleccionados: List<Pair<Int, Int>> = emptyList()
    ) : SeatsUiState()

    object Bloqueando : SeatsUiState()

    object Bloqueados : SeatsUiState()

    object Vendiendo : SeatsUiState()

    object Exito : SeatsUiState()

    data class Error(val mensaje: String) : SeatsUiState()
}
