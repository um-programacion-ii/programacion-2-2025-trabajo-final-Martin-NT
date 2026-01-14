package ar.edu.um.tpfinal.dto
import kotlinx.serialization.Serializable

@Serializable
data class AsientoUbicacionDTO(
    val fila: Int,
    val columna: Int

)
