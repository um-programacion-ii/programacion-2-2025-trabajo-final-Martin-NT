package ar.edu.um.tpfinal.dto

import kotlinx.serialization.Serializable

@Serializable
data class AsientoBloqueoRequestDTO(
    val eventoId: Long,
    val asientos: List<AsientoUbicacionDTO>
)
