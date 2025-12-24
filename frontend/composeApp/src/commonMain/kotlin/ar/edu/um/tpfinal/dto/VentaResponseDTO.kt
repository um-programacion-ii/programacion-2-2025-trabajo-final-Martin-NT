package ar.edu.um.tpfinal.dto

import kotlinx.serialization.Serializable

@Serializable
data class VentaResponseDTO(
    private val eventoId: Long,
    private val ventaId: Long,
    private val fechaVenta: String,
    private val asientos: List<VentaAsientoDTO>,
    private val resultado: Boolean,
    private val descripcion: String,
    private val precioVenta: Double
)
