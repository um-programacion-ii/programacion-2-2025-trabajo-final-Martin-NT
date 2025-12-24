package ar.edu.um.tpfinal.viewmodel

/**
 * Estado de UI para LoginScreen.
 * - Inactivo: pantalla normal
 * - Cargando: esperando respuesta del backend
 * - Exito: login ok (navegar)
 * - Error: mostrar mensaje
 */
sealed class LoginUiState {
    object Inactivo : LoginUiState()
    object Cargando : LoginUiState()
    data class Exito(val token: String) : LoginUiState()
    data class Error(val mensaje: String) : LoginUiState()
}
