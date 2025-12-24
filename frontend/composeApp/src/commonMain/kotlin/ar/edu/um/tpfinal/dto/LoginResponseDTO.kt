package ar.edu.um.tpfinal.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponseDTO(
    val id_token : String,
)
