package ar.edu.um.tpfinal.viewmodel
/**
 * Representa el ESTADO de LA UI de la pantalla de Login.
 *
 * Objetivo:
 * - Modelar explícitamente todos los estados posibles de la pantalla.
 * - Evitar booleanos sueltos (loading, error, etc.).
 * - Permitir que la UI reaccione con un simple `when(state)`.
 *
 * Este UiState es consumido por LoginViewModel
 * y observado por LoginScreen.
 */
sealed class LoginUiState {

    object Inactivo : LoginUiState() // Estado inicial.

    object Cargando : LoginUiState() // Estado transitorio mientras se espera respuesta del backend.

    data class Exito( // Login confirmado por el backend.
        val token: String // token JWT devuelto por el backend.
    ) : LoginUiState()

    data class Error( // Error durante el proceso de login.
        val mensaje: String
    ) : LoginUiState()
}
