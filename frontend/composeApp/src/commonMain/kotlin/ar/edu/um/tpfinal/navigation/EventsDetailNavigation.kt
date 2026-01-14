package ar.edu.um.tpfinal.navigation
import androidx.compose.runtime.Composable
import ar.edu.um.tpfinal.dto.EventoResumenDTO
import ar.edu.um.tpfinal.screens.EventsDetailScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
/**
 * Pantalla de navegación que representa la ruta
 * hacia el detalle de un evento.
 *
 * Su responsabilidad es:
 *    1) Recibir los datos necesarios para abrir la pantalla (EventoResumenDTO).
 *    2) Construir/mostrar la Screen real (EventsDetailScreen).
 *    3) Resolver acciones de navegación (volver atrás).
 *
 * Importante:
 * - Esta clase NO llama al backend.
 * - Esta clase NO maneja estados (ViewModel).
 * - Solo conecta navegación → pantalla.
 */
data class EventsDetailNavigation(
    // Dato que necesita la pantalla de detalle para renderizar.
    // Se pasan desde la pantalla anterior al hacer navigator.push(...)
    val event: EventoResumenDTO
) : Screen {

    @Composable
    override fun Content() {

        // Obtenemos el navigator actual (stack de pantallas).
        val navigator = LocalNavigator.currentOrThrow

        // Renderiza la pantalla de detalle, inyectando:
        // - el evento a mostrar
        // - el callback de volver (pop del stack)
        EventsDetailScreen(
            event = event,
            onBack = { navigator.pop() } // vuelve a la pantalla anterior
        )
    }
}
