package ar.edu.um.tpfinal.dto
import kotlinx.serialization.Serializable

@Serializable
data class AsientoEstadoDTO (
    val fila: Int,
    val columna: Int,
    val estado: String,
    val expira: String
)