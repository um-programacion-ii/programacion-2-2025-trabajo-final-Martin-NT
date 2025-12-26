package ar.edu.um.tpfinal.navigation
import androidx.compose.runtime.Composable
import ar.edu.um.tpfinal.screens.SeatsScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
/**
 * Pantalla de navegación que representa la ruta de selección
 * y venta de asientos para un evento.
 *
 * - Recibe el eventoId (externalId).
 * - Renderiza SeatsScreen.
 * - Define la navegación de salida (volver o finalizar).
 *
 * Importante:
 * - NO maneja estados de asientos.
 * - NO llama al backend.
 * - Solo conecta la pantalla SeatsScreen con el stack de navegación.
 */
data class SeatsNavigation(
    val eventoId: Long   // externalId del evento
) : Screen {

    @Composable
    override fun Content() {

        // Controla el stack de navegación (push / pop)
        val navigator = LocalNavigator.currentOrThrow

        SeatsScreen(
            eventoId = eventoId,

            onBack = { navigator.pop() },

            /**
             * Luego de una venta confirmada:
             * - se vuelve a la pantalla anterior
             */
            onSuccess = { navigator.pop() }
        )
    }
}
