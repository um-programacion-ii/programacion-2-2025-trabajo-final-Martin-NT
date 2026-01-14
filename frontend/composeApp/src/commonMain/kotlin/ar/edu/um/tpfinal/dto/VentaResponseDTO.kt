package ar.edu.um.tpfinal.dto

import kotlinx.serialization.Serializable

@Serializable
data class VentaResponseDTO(
    val eventoId: Long,
    val ventaId: Long,
    val fechaVenta: String,
    val asientos: List<VentaAsientoDTO>,
    val resultado: Boolean,
    val descripcion: String,
    val precioVenta: Double
)
