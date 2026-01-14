package ar.edu.um.tpfinal.dto

import kotlinx.serialization.Serializable

@Serializable
data class AsientoBloqueoResponseDTO(
    val eventoId: Long,
    val resultado: Boolean,
    val descripcion: String,
    val asientos: List<AsientoEstadoDTO>
)
