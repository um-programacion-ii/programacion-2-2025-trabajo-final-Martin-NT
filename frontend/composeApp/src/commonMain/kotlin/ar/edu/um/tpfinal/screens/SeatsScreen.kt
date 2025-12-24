package ar.edu.um.tpfinal.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.edu.um.tpfinal.dto.AsientoEstadoDTO
import ar.edu.um.tpfinal.viewmodel.SeatsUiState
import ar.edu.um.tpfinal.viewmodel.SeatsViewModel
import kotlinx.coroutines.launch

@Composable
fun SeatsScreen(
    eventoId: Long,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val viewModel = remember { SeatsViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Estado local para capturar el nombre de la persona
    var nombrePersona by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.cargarMapa(eventoId) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCDD3D5))
    ) {
        when (val estado = uiState) {
            SeatsUiState.CargandoAsientos,
            SeatsUiState.Bloqueando,
            SeatsUiState.Vendiendo -> {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            is SeatsUiState.AsientosCargados -> {
                val asientos: List<AsientoEstadoDTO> = estado.asientos
                var columnas = 1
                for (a in asientos) { if (a.columna > columnas) columnas = a.columna }

                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp, start = 8.dp, end = 16.dp, bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                            Text("❮ Atrás", fontSize = 14.sp, color = Color.DarkGray)
                        }
                        Text(text = "Asientos Disponibles", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Seleccioná hasta 4 asientos", color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                LegendItem("Libres", Color(0xFF4CAF50))
                                LegendItem("Seleccionados", Color(0xFF2196F3))
                                LegendItem("Ocupados", Color(0xFFF44336))
                                LegendItem("Bloqueados", Color.Gray)
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columnas),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            ) {
                                items(items = asientos, key = { "${it.fila}-${it.columna}" }) { asiento ->
                                    val seleccionado = estado.seleccionados.contains(asiento.fila to asiento.columna)
                                    SeatIcon(asiento = asiento, isSeleccionado = seleccionado, onClick = { viewModel.alternarSeleccion(asiento.fila, asiento.columna) })
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = { scope.launch { viewModel.bloquearSeleccionados(eventoId) } },
                                enabled = estado.seleccionados.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))
                            ) {
                                Text("BLOQUEAR SELECCIONADOS (${estado.seleccionados.size})")
                            }
                        }
                    }
                }
            }

            SeatsUiState.Bloqueados -> {
                // PANTALLA CARGA DE DATOS
                Card(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Confirmar Reserva", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = nombrePersona,
                            onValueChange = { nombrePersona = it },
                            label = { Text("Nombre de la persona") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = { scope.launch { viewModel.venderAsientos(eventoId, nombrePersona) } },
                            enabled = nombrePersona.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("COMPRAR AHORA")
                        }

                        TextButton(onClick = { onBack() }) {
                            Text("Volver al inicio", color = Color.Gray)
                        }
                    }
                }
            }

            is SeatsUiState.Exito -> {
                // PANTALLA DE VENTA REALIZADA
                val venta = estado.venta

                Card(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Descripción: "Venta realizada con éxito"
                        Text(
                            text = venta.descripcion,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF2E7D32)

                        )

                        Spacer(Modifier.height(16.dp))

                        // Persona (Usamos el nombre que cargamos en el paso anterior)
                        Text("Persona: $nombrePersona", fontWeight = FontWeight.SemiBold)

                        Spacer(Modifier.height(8.dp))

                        // Formatear la lista de asientos para que sea legible
                        // Ejemplo: "Fila 5 Col 3, Fila 5 Col 4"
                        val textoAsientos = venta.asientos.joinToString(", ") { "Fila ${it.fila} Col ${it.columna}" }

                        Text(
                            text = "Asientos: $textoAsientos",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        )

                        Spacer(Modifier.height(12.dp))

                        // Precio Total
                        Text(
                            text = "Total: $${venta.precioVenta}",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = { onSuccess() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("FINALIZAR", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is SeatsUiState.Error -> {
                Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = estado.mensaje, color = Color.Red, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("Volver") }
                }
            }
            else -> Unit
        }
    }
}

// SeatIcon y LegendItem se mantienen iguales...
@Composable
fun SeatIcon(asiento: AsientoEstadoDTO, isSeleccionado: Boolean, onClick: () -> Unit) {
    val color = when {
        isSeleccionado -> Color(0xFF2196F3)
        asiento.estado == "LIBRE" -> Color(0xFF4CAF50)
        asiento.estado == "VENDIDO" -> Color(0xFFF44336)
        else -> Color.Gray
    }
    Box(
        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
            .background(color).clickable(enabled = asiento.estado == "LIBRE") { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxWidth(0.6f).height(4.dp).align(Alignment.TopCenter).padding(top = 2.dp).background(Color.White.copy(alpha = 0.3f)))
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color.DarkGray)
    }
}