package ar.edu.um.tpfinal.dto
import kotlinx.serialization.Serializable

@Serializable
data class EventoDetalleDTO (
    val id: Long,
    val titulo: String,
    val resumen: String,
    val descripcion: String,
    val fecha: String,
    val direccion: String,
    val imagen: String,
    val filaAsientos: Int,
    val columnaAsientos: Int,
    val precioEntrada: Double,
    val eventoTipo: EventoTipoDTO,
    val integrantes: List<EventoIntegrantesDTO>,

)