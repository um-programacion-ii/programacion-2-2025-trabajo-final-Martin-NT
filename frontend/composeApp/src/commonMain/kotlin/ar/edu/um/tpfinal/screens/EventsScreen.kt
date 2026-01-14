package ar.edu.um.tpfinal.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.edu.um.tpfinal.dto.EventoResumenDTO
import ar.edu.um.tpfinal.viewmodel.EventsViewModel
import kotlinx.coroutines.launch

@Composable
fun EventsScreen(
    onEventClick: (EventoResumenDTO) -> Unit,
    onLogout: () -> Unit // Agregado para la lógica de cierre
) {
    val viewModel = remember { EventsViewModel() }
    val scope = rememberCoroutineScope()
    val events by viewModel.events.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadEvents(force = true)
    }

    // Contenedor principal con el color de fondo personalizado
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCDD3D5))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- Cabecera ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp,
                        bottom = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón cerrar sesión
                TextButton(
                    onClick = onLogout,
                    contentPadding = PaddingValues(6.dp)
                ) {
                    Text("Cerrar Sesión", fontSize = 12.sp, color = Color.DarkGray)
                }
            }

            // Título principal
            Text(
                text = "Eventos",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                textAlign = TextAlign.Center
            )

            // --- CONTENIDO PRINCIPAL ---
            Box(modifier = Modifier.fillWeight(1f)) {
                when {
                    loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    error != null -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Error: $error", color = Color.Red, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { scope.launch { viewModel.loadEvents() } }) {
                                Text("Reintentar")
                            }
                        }
                    }
                    events.isEmpty() -> {
                        Text(
                            text = "No hay eventos disponibles",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp) // Espacio entre recuadros
                        ) {
                            items(items = events, key = { it.id }) { event ->
                                EventItem(
                                    event = event,
                                    onClick = { onEventClick(event) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventItem(
    event: EventoResumenDTO,
    onClick: () -> Unit
) {
    // Recuadros blancos (Cards)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Nombre del Evento
            Text(
                text = event.titulo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Fila inferior
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fecha
                Text(
                    text = "Fecha: ${event.fecha}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                // Precio
                Text(
                    text = "$${event.precioEntrada}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF2E7D32), // Color verde para el precio
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Extensión rápida para manejar el peso del layout
fun Modifier.fillWeight(weight: Float) = this.then(Modifier.fillMaxWidth().fillMaxHeight(weight))