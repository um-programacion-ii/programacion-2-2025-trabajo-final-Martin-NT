package ar.edu.um.tpfinal.dto

import kotlinx.serialization.Serializable

@Serializable
data class VentaRequestDTO(
    val eventoId: Long,
    val asientos: List<VentaAsientoFrontendDTO>


)
