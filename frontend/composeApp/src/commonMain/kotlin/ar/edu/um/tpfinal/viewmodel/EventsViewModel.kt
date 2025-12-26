package ar.edu.um.tpfinal.viewmodel
import ar.edu.um.tpfinal.dto.EventoResumenDTO
import ar.edu.um.tpfinal.network.ApiClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
/**
 * ViewModel responsable de la pantalla de listado de eventos.
 *
 * Rol:
 * - Coordina la carga de eventos desde el backend (vía ApiClient).
 * - Expone el estado observable de la UI usando StateFlow:
 *     - lista de eventos
 *     - estado de carga
 *     - mensaje de error
 *
 * Importante:
 * - NO contiene lógica de red directa (eso vive en ApiClient).
 * - NO conoce detalles de backend/proxy.
 * - Se enfoca únicamente en estado y flujo de UI.
 */
class EventsViewModel {

    // UI STATE (StateFlow)

    // Lista de eventos resumidos a mostrar en pantalla
    private val _events = MutableStateFlow<List<EventoResumenDTO>>(emptyList())
    val events: StateFlow<List<EventoResumenDTO>> = _events.asStateFlow()

    // Indica si la pantalla está cargando datos
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    // Mensaje de error a mostrar en UI (Snackbar, Text, etc.)
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()


    // CONTROL DE CARGA

    // Evita recargar eventos innecesariamente
    private var yaCargado = false

    /**
     * Carga la lista de eventos resumidos desde el backend.
     * @param force si es true, fuerza la recarga aunque ya se haya cargado antes.
     */
    suspend fun loadEvents(force: Boolean = false) {

        // Evita recargar si ya se cargó y no se fuerza
        if (yaCargado && !force) return
        yaCargado = true

        // [UI] Estado inicial de carga
        _loading.value = true
        _error.value = null

        // Verifica que el token JWT esté presente
        println("Token guardado: ${ApiClient.getToken()?.take(50)}...")
        println("Cargando eventos (resumen) ...")

        // Llamada al ApiClient (backend)
        val result = ApiClient.getEvents()

        // Manejo explícito de éxito / error
        result.fold(
            onSuccess = { eventList ->
                println("✅ Eventos cargados exitosamente: ${eventList.size}")

                // Se actualiza la UI con la lista recibida
                _events.value = eventList
            },
            onFailure = { err ->
                println("❌ Error cargando eventos: ${err.message}")
                err.printStackTrace()

                // Traduce excepciones técnicas a mensajes de UI
                val message = when (err) {
                    is HttpRequestTimeoutException -> "Tiempo de espera agotado"
                    is ConnectTimeoutException -> "No se pudo conectar al servidor"
                    is SocketTimeoutException -> "Conexión perdida"
                    else -> err.message ?: "Error al cargar eventos"
                }

                // Error visible en UI
                _error.value = message
                _events.value = emptyList()
            }
        )

        // Fin del estado de carga
        _loading.value = false
    }
}
