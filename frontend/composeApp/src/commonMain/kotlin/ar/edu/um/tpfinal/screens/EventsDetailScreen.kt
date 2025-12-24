package ar.edu.um.tpfinal.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
            .background(Color(0xFFCDD3D5))
    ) {
        // --- CABECERA ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, start = 8.dp, end = 16.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center // Esto centra el contenido del Box
        ) {
            // Botón "Atrás" alineado a la izquierda
            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("❮ Atrás", fontSize = 14.sp, color = Color.DarkGray)
            }

            // Título centrado
            Text(
                text = "Detalle del evento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
        }

        // --- CUERPO ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = event.titulo,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )

                Spacer(Modifier.height(16.dp))

                // Etiqueta Tipo
                Surface(
                    color = Color(0xFFCDD3D5).copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = event.eventoTipo.nombre,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.DarkGray
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("FECHA", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(event.fecha, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("PRECIO", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("$${event.precioEntrada}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 18.sp)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), thickness = 0.5.dp)

                Text(text = "Sobre este evento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(8.dp))

                Text(
                    text = event.descripcion.ifBlank { event.descripcion },
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { navigator.push(SeatsNavigation(eventoId = event.id)) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))
                ) {
                    Text("VER ASIENTOS DISPONIBLES", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}