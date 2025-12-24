package ar.edu.um.tpfinal.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

    LaunchedEffect(Unit) {
        viewModel.cargarMapa(eventoId)
    }

    LaunchedEffect(uiState) {
        if (uiState is SeatsUiState.Exito) {
            onSuccess()
        }
    }

    when (val estado = uiState) {

        SeatsUiState.CargandoAsientos -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is SeatsUiState.AsientosCargados -> {

            // ✅ Calcular columnas sin maxOf/maxByOrNull (KMP friendly)
            val asientos: List<AsientoEstadoDTO> = estado.asientos
            var columnas = 1
            for (a in asientos) {
                if (a.columna > columnas) columnas = a.columna
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header con volver
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 22.sp)
                    }
                    Text(
                        text = "Mapa de asientos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Seleccioná hasta 4 asientos",
                    color = Color.Gray
                )

                Spacer(Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnas),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = asientos,
                        key = { a -> "${a.fila}-${a.columna}" }
                    ) { asiento ->
                        val seleccionado =
                            estado.seleccionados.contains(asiento.fila to asiento.columna)

                        val color = when {
                            seleccionado -> Color.Blue
                            asiento.estado == "LIBRE" -> Color.Green
                            asiento.estado.startsWith("BLOQUEADO") -> Color.Gray
                            asiento.estado == "VENDIDO" -> Color.Red
                            else -> Color.LightGray
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(color)
                                .clickable(enabled = asiento.estado == "LIBRE") {
                                    viewModel.alternarSeleccion(asiento.fila, asiento.columna)
                                }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { scope.launch { viewModel.bloquearSeleccionados(eventoId) } },
                    enabled = estado.seleccionados.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Bloquear seleccionados")
                }
            }
        }

        SeatsUiState.Bloqueando,
        SeatsUiState.Vendiendo -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        SeatsUiState.Bloqueados -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✅ Asientos bloqueados correctamente", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { onBack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Volver")
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        scope.launch {
                            // Placeholder: después lo reemplazamos por pantalla de carga de personas
                            viewModel.venderAsientos(
                                eventoId = eventoId,
                                persona = "user"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Comprar")
                }
            }
        }

        is SeatsUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = estado.mensaje, color = Color.Red)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Volver")
                }
            }
        }

        else -> Unit
    }
}
