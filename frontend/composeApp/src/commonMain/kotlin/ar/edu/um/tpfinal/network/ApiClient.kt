package ar.edu.um.tpfinal.network

import ar.edu.um.tpfinal.config.ApiConfig
import ar.edu.um.tpfinal.dto.AsientoBloqueoRequestDTO
import ar.edu.um.tpfinal.dto.AsientoBloqueoResponseDTO
import ar.edu.um.tpfinal.dto.AsientoEstadoDTO
import ar.edu.um.tpfinal.dto.AsientoUbicacionDTO
import ar.edu.um.tpfinal.dto.EventoResumenDTO
import ar.edu.um.tpfinal.dto.LoginRequestDTO
import ar.edu.um.tpfinal.dto.LoginResponseDTO
import ar.edu.um.tpfinal.dto.VentaRequestDTO
import ar.edu.um.tpfinal.dto.VentaResponseDTO
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ApiClient {
    private var jwtToken: String? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 15000
        }

        expectSuccess = false

        defaultRequest {
            url(ApiConfig.baseUrl)
            contentType(ContentType.Application.Json)
        }
    }

    fun setTokens(jwt: String) {
        jwtToken = jwt
        println("💾 JWT guardado: ${jwt.take(30)}...")
    }

    fun getToken(): String? = jwtToken

    private fun HttpRequestBuilder.addAuth() {
        val token = jwtToken
        if (token.isNullOrBlank()) {
            println("⚠️ Falta token JWT (no se agregará Authorization)")
            return
        }
        header("Authorization", "Bearer $token")
    }

    suspend fun login(request: LoginRequestDTO): Result<String> {
        return try {
            println("POST /api/authenticate")
            println("Request body: ${json.encodeToString(request)}")

            val response: HttpResponse = client.post("/api/authenticate") {
                setBody(request)
            }

            println("Status code: ${response.status.value}")

            if (response.status.value in 200..299) {
                val loginResponseDTO: LoginResponseDTO = response.body()

                setTokens(loginResponseDTO.id_token)

                println("✅ Login exitoso!")
                Result.success(loginResponseDTO.id_token)
            } else {
                val errorBody = response.bodyAsText()
                println("❌ Error: ${response.status} - $errorBody")
                Result.failure(Exception("Error ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            println("💥 Excepción login: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getEvents(): Result<List<EventoResumenDTO>> {
        return try {
            println("GET /api/eventos/resumidos")

            val response: HttpResponse = client.get("/api/eventos/resumidos") {
                addAuth()
            }

            println("Status code: ${response.status.value}")

            if (response.status.value in 200..299) {
                val events: List<EventoResumenDTO> = response.body()
                println("${events.size} eventos resumidos cargados")
                Result.success(events)
            } else {
                val errorBody = response.bodyAsText()
                println("❌ Error ${response.status.value}: $errorBody")
                Result.failure(Exception("Error ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            println("💥 Excepción getEvents: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getAsientosEvento(eventoId: Long): Result<List<AsientoEstadoDTO>> {
        return try {
            println("GET /api/eventos/$eventoId/asientos")

            val response: HttpResponse = client.get("/api/eventos/$eventoId/asientos") {
                addAuth()
            }

            println("Status code: ${response.status.value}")

            if (response.status.value in 200..299) {
                val asientos: List<AsientoEstadoDTO> = response.body()
                Result.success(asientos)
            } else {
                val errorBody = response.bodyAsText()
                println("❌ Error ${response.status.value}: $errorBody")
                Result.failure(Exception("Error ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            println("💥 Excepción getAsientosEvento: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun bloquearAsientos(
        eventoId: Long,
        asientos: List<AsientoUbicacionDTO>
    ): Result<AsientoBloqueoResponseDTO> {
        return try {
            println("POST /api/eventos/$eventoId/bloqueos")
            println("Request body (asientos): ${json.encodeToString(asientos)}")

            val request = AsientoBloqueoRequestDTO(
                eventoId = eventoId,
                asientos = asientos.map { AsientoUbicacionDTO(it.fila, it.columna) }
            )

            val response: HttpResponse = client.post("/api/eventos/$eventoId/bloqueos") {
                addAuth()
                setBody(request)
            }

            println("Status code: ${response.status.value}")

            if (response.status.value in 200..299) {
                val bloqueoResponse: AsientoBloqueoResponseDTO = response.body()
                println("✅ Bloqueo exitoso!")
                Result.success(bloqueoResponse)
            } else {
                val errorBody = response.bodyAsText()
                println("❌ Error: ${response.status} - $errorBody")
                Result.failure(Exception("Error ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            println("💥 Excepción bloqueo: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun venderAsientos(eventoId: Long, request: VentaRequestDTO): Result<VentaResponseDTO> {
        return try {
            println("POST /api/ventas/$eventoId/venta")
            println("Request body: ${json.encodeToString(request)}")

            val response: HttpResponse = client.post("/api/ventas/$eventoId/venta") {
                addAuth()
                setBody(request)
            }

            println("Status code: ${response.status.value}")

            if (response.status.value in 200..299) {
                val ventaResponseDTO: VentaResponseDTO = response.body()
                println("✅ Venta exitosa!")
                Result.success(ventaResponseDTO)
            } else {
                val errorBody = response.bodyAsText()
                println("❌ Error: ${response.status} - $errorBody")
                Result.failure(Exception("Error ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            println("💥 Excepción venta: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
