package ar.edu.um.tpfinal.dto

import kotlinx.serialization.Serializable

@Serializable
data class VentaAsientoFrontendDTO(
    val fila: Int,
    val columna: Int,
    val persona: String,
)
