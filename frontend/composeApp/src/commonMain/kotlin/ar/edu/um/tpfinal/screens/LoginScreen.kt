package ar.edu.um.tpfinal.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ar.edu.um.tpfinal.viewmodel.LoginUiState
import ar.edu.um.tpfinal.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit // Callback para navegar luego del login OK
) {
    // Maneja el estado del login (Inactivo / Cargando / Exito / Error)
    val viewModel = remember { LoginViewModel() }

    // Usado para lanzar la llamada de login
    val scope = rememberCoroutineScope()

    // ESTADO LOCAL DE CAMPOS
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // La UI se redibuja automáticamente cuando cambia
    val state by viewModel.uiState.collectAsState()

    // Evita navegar múltiples veces si el estado cambia
    var navegado by remember { mutableStateOf(false) }

    /**
     * Cuando el estado pasa a Exito:
     * - se ejecuta onLoginSuccess()
     * - se navega una sola vez
     */
    LaunchedEffect(state) {
        if (!navegado && state is LoginUiState.Exito) {
            navegado = true
            onLoginSuccess()
        }
    }

    val cargando = state is LoginUiState.Cargando
    val camposValidos = username.isNotBlank() && password.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCDD3D5))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // TÍTULO
                Text(
                    text = "Iniciar Sesión",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                Text(
                    text = "Usuario",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("Ingresá tu usuario") },
                    singleLine = true,
                    enabled = !cargando,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Contraseña",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Ingresá tu contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = !cargando,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                // BOTÓN LOGIN
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.login(username.trim(), password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !cargando && camposValidos,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF34A853),
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.large
                ) {

                    if (cargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Entrar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (state) {
                    is LoginUiState.Error -> {
                        Text(
                            text = (state as LoginUiState.Error).mensaje,
                            color = Color(0xFFD32F2F),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> Unit
                }
            }
        }
    }
}
