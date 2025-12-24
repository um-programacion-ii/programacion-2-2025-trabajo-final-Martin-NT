package ar.edu.um.tpfinal.navigation
import androidx.compose.runtime.Composable
import ar.edu.um.tpfinal.screens.EventsScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class EventsNavigation : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        EventsScreen(
            onEventClick = { event ->
                navigator.push(EventsDetailNavigation(event))
            },
            onLogout = {
                navigator.replace(LoginNavigation())
            }
        )
    }
}
