package ar.edu.um.tpfinal.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.edu.um.tpfinal.dto.EventoResumenDTO
import ar.edu.um.tpfinal.navigation.SeatsNavigation
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

@Composable
fun EventsDetailScreen(
    event: EventoResumenDTO,
    onBack: () -> Unit
) {
    val navigator = LocalNavigator.currentOrThrow

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 22.sp)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Detalle del evento",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = event.titulo,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = event.resumen.ifBlank { event.descripcion },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(12.dp))

        Text("Fecha: ${event.fecha}", color = Color.Gray)
        Text("Tipo: ${event.eventoTipo.nombre}", color = Color.Gray)
        Text("Precio: $${event.precioEntrada}", fontWeight = FontWeight.Bold)

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { navigator.push(SeatsNavigation(eventoId = event.id.toLong())) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ver mapa de asientos")
        }
    }
}
