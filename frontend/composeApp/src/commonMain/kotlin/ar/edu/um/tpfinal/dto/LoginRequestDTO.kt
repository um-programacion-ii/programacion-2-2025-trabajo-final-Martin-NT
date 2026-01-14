package ar.edu.um.tpfinal.dto
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDTO(
    val username: String,
    val password: String
)