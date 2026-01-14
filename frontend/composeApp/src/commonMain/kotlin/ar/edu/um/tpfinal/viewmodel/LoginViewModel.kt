package ar.edu.um.tpfinal.viewmodel
import ar.edu.um.tpfinal.dto.LoginRequestDTO
import ar.edu.um.tpfinal.network.ApiClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
/**
 * ViewModel responsable del flujo de autenticación (login).
 *
 * Rol:
 * - Gestiona el estado de la pantalla de login.
 * - Valida datos básicos de entrada (username / password).
 * - Llama al backend a través de ApiClient.
 * - Traduce resultados técnicos en estados de UI claros.
 *
 * Importante:
 * - NO dibuja UI.
 * - NO conoce detalles visuales.
 * - NO maneja navegación directamente.
 */
class LoginViewModel {

    // UI STATE

    // Estado observable de la pantalla de login
    // Puede ser: Inactivo, Cargando, Exito, Error
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Inactivo)

    // Estado inmutable para la UI
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Ejecuta el login contra el backend.
     *
     * Flujo:
     * 1) Valida campos obligatorios.
     * 2) Cambia el estado a Cargando.
     * 3) Llama a ApiClient.login().
     * 4) Actualiza UiState según éxito o error.
     */
    suspend fun login(username: String, password: String) {

        // Campos requeridos antes de llamar al backend
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Usuario y contraseña son requeridos")
            return
        }

        // [UI] Estado de carga
        _uiState.value = LoginUiState.Cargando

        // Llamada al backend vía ApiClient
        val result = ApiClient.login(LoginRequestDTO(username, password))

        // Manejo explícito de éxito / error
        result.fold(
            onSuccess = { token ->
                // Login confirmado por el backend
                // token = JWT real devuelto
                _uiState.value = LoginUiState.Exito(token)
            },
            onFailure = { error ->
                // Traduce errores técnicos a mensajes de UI
                val mensaje = when (error) {
                    is HttpRequestTimeoutException -> "Tiempo de espera agotado"
                    is ConnectTimeoutException -> "No se pudo conectar al servidor"
                    is SocketTimeoutException -> "Conexión perdida"
                    else -> error.message ?: "Error en el login"
                }
                _uiState.value = LoginUiState.Error(mensaje)
            }
        )
    }

    /**
     * Resetea el estado de la pantalla a Inactivo.
     *
     * Se usa, por ejemplo:
     * - al volver a mostrar la pantalla de login
     * - después de mostrar un error
     * - al cerrar sesión
     */
    fun resetState() {
        _uiState.value = LoginUiState.Inactivo
    }
}
