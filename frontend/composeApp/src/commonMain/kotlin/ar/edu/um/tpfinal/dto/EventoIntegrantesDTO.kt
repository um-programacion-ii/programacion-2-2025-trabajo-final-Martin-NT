package ar.edu.um.tpfinal.dto

import kotlinx.serialization.Serializable

@Serializable
data class EventoIntegrantesDTO(
    val nombre: String,
    val apellido: String,
    val identificacion: String
)
