package ar.edu.um.tpfinal.navigation
import androidx.compose.runtime.Composable
import ar.edu.um.tpfinal.screens.EventsScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
/**
 * Pantalla de navegación que representa la ruta
 * de "Listado de eventos".
 *
 * Define qué pasa cuando:
 *    1) el usuario selecciona un evento (navegar a detalle),
 *    2) el usuario toca logout (volver a login).
 *
 * Importante:
 * - NO contiene lógica de negocio.
 * - NO llama al backend.
 * - La lógica de datos vive en EventsScreen/ViewModel.
 * - Esto es solo wiring de navegación.
 */
class EventsNavigation : Screen {

    @Composable
    override fun Content() {

        // Maneja el stack de pantallas (push/pop/replace)
        val navigator = LocalNavigator.currentOrThrow

        EventsScreen(
            /**
             * Cuando el usuario toca un evento:
             * - se hace push al detalle
             * - se pasa el DTO del evento seleccionado como argumento
             */
            onEventClick = { event ->
                navigator.push(EventsDetailNavigation(event))
            },

            /**
             * Cuando el usuario cierra sesión:
             * - reemplazamos la pila actual por Login
             * - replace evita que al "volver" regrese a eventos
             */
            onLogout = {
                navigator.replace(LoginNavigation())
            }
        )
    }
}
