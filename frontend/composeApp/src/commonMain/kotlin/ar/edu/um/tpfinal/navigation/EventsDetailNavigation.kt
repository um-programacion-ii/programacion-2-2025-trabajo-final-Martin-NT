package ar.edu.um.tpfinal.navigation
import androidx.compose.runtime.Composable
import ar.edu.um.tpfinal.dto.EventoResumenDTO
import ar.edu.um.tpfinal.screens.EventsDetailScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

data class EventsDetailNavigation(
    val event: EventoResumenDTO
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        EventsDetailScreen(
            event = event,
            onBack = { navigator.pop() }
        )
    }
}
