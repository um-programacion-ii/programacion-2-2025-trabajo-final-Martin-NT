package ar.edu.um.tpfinal.dto

import kotlinx.serialization.Serializable

@Serializable
data class EventoTipoDTO(
    val nombre: String,
    val descripcion: String
)
