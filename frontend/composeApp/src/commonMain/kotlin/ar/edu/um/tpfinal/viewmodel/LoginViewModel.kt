package ar.edu.um.tpfinal.viewmodel

import ar.edu.um.tpfinal.dto.LoginRequestDTO
import ar.edu.um.tpfinal.network.ApiClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Inactivo)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    suspend fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Usuario y contraseña son requeridos")
            return
        }

        _uiState.value = LoginUiState.Cargando

        val result = ApiClient.login(LoginRequestDTO(username, password))

        result.fold(
            onSuccess = { token ->
                // token real devuelto por el backend
                _uiState.value = LoginUiState.Exito(token)
            },
            onFailure = { error ->
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

    fun resetState() {
        _uiState.value = LoginUiState.Inactivo
    }
}
