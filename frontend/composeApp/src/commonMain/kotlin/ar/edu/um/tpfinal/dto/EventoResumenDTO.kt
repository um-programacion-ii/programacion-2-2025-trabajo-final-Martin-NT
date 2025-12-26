package ar.edu.um.tpfinal.dto

import kotlinx.serialization.Serializable

@Serializable
data class EventoResumenDTO (
    val id: Long,
    val titulo: String,
    val resumen: String,
    val descripcion: String,
    val fecha: String,
    val precioEntrada: Double,
    val eventoTipo: EventoTipoDTO,

)