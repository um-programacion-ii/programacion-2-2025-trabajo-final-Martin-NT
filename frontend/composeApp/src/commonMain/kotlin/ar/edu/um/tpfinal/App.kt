package ar.edu.um.tpfinal

import androidx.compose.runtime.Composable
import ar.edu.um.tpfinal.navigation.LoginNavigation
import cafe.adriel.voyager.navigator.Navigator
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    Navigator(LoginNavigation())
}
