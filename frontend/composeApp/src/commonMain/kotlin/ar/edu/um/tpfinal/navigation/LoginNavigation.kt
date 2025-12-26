package ar.edu.um.tpfinal.navigation
import androidx.compose.runtime.Composable
import ar.edu.um.tpfinal.screens.LoginScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
/**
 * Pantalla de navegación que representa la ruta de Login.
 *
 * Define qué ocurre cuando el login es exitoso.
 *
 * Importante:
 * - NO valida credenciales.
 * - NO llama al backend.
 * - Solo maneja la navegación posterior al login.
 */
class LoginNavigation : Screen {

    @Composable
    override fun Content() {

        // Controla la pila de pantallas (stack)
        val navigator = LocalNavigator.currentOrThrow

        LoginScreen(
            /**
             * Cuando el login es exitoso:
             * - Se reemplaza TODA la pila por EventsNavigation
             * - El usuario NO puede volver atrás al login
             */
            onLoginSuccess = {
                navigator.replaceAll(EventsNavigation())
            }
        )
    }
}
