package ar.edu.um.tpfinal.viewmodel

import ar.edu.um.tpfinal.dto.EventoResumenDTO
import ar.edu.um.tpfinal.network.ApiClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EventsViewModel {

    private val _events = MutableStateFlow<List<EventoResumenDTO>>(emptyList())
    val events: StateFlow<List<EventoResumenDTO>> = _events.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var yaCargado = false

    suspend fun loadEvents(force: Boolean = false) {
        if (yaCargado && !force) return
        yaCargado = true

        _loading.value = true
        _error.value = null

        println("Token guardado: ${ApiClient.getToken()?.take(50)}...")
        println("Cargando eventos (resumen) ...")

        val result = ApiClient.getEvents()

        result.fold(
            onSuccess = { eventList ->
                println("✅ Eventos cargados exitosamente: ${eventList.size}")
                _events.value = eventList
            },
            onFailure = { err ->
                println("❌ Error cargando eventos: ${err.message}")
                err.printStackTrace()

                val message = when (err) {
                    is HttpRequestTimeoutException -> "Tiempo de espera agotado"
                    is ConnectTimeoutException -> "No se pudo conectar al servidor"
                    is SocketTimeoutException -> "Conexión perdida"
                    else -> err.message ?: "Error al cargar eventos"
                }

                _error.value = message
                _events.value = emptyList()
            }
        )

        _loading.value = false
    }
}
