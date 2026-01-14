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
/**
 * Cliente HTTP central del FRONTEND (KMP).
 *
 * Rol:
 * - Consume EXCLUSIVAMENTE endpoints del BACKEND del alumno.
 * - El frontend NO conoce ni accede al proxy ni a Redis.
 *
 * Flujo real:
 *   Frontend → Backend → Proxy-Service → Redis / Cátedra
 *
 * Este cliente:
 * - Centraliza llamadas REST al backend.
 * - Maneja JWT (login + Authorization Bearer).
 * - No contiene lógica de negocio (eso vive en ViewModels).
 */

object ApiClient {

    // Token JWT (en memoria). Se setea luego del login.
    private var jwtToken: String? = null

    // Configuración de parseo JSON:
    private val json = Json {
        ignoreUnknownKeys = true // tolera campos extra del backend sin romper.
        isLenient = true // permite JSON menos estricto (útil para compatibilidad).
        prettyPrint = true // solo afecta logs/serialización, no la lógica.
    }

    // Cliente Ktor central.
    // Configurado una sola vez: plugins + baseUrl + content-type por defecto.
    private val client = HttpClient {

        install(ContentNegotiation) {
            json(json)
        }

        // [PLUGIN] Timeouts para evitar requests colgadas
        install(HttpTimeout) {
            requestTimeoutMillis = 30000  // tiempo total de request
            connectTimeoutMillis = 15000  // tiempo para conectar
            socketTimeoutMillis = 15000   // tiempo de espera de lectura/escritura
        }

        // No tirar excepción automática por 4xx/5xx.
        expectSuccess = false

        // Se aplica a todos los requests:
        defaultRequest {
            url(ApiConfig.baseUrl)
            contentType(ContentType.Application.Json)
        }
    }

    // Guarda el JWT en memoria para usarlo en requests protegidos.
    fun setTokens(jwt: String) {
        jwtToken = jwt
        println("💾 JWT guardado: ${jwt.take(30)}...")
    }

    // Devuelve el JWT actual (si existe).
    fun getToken(): String? = jwtToken

    // Helper: agrega Authorization: Bearer <token> a un request si hay JWT.
    private fun HttpRequestBuilder.addAuth() {
        val token = jwtToken
        if (token.isNullOrBlank()) {
            println("⚠️ Falta token JWT (no se agregará Authorization)")
            return
        }
        header("Authorization", "Bearer $token")
    }

    /**
     * Backend endpoint: POST /api/authenticate
     *
     * Función:
     * - Envía credenciales (LoginRequestDTO) al BACKEND.
     *      - Si es OK (2xx) recibe LoginResponseDTO con id_token.
     * - El backend autentica y devuelve JWT.
     * - El frontend guarda el token para llamadas posteriores.
     */

    suspend fun login(request: LoginRequestDTO): Result<String> {
        return try {
            println("POST /api/authenticate")
            println("Request body: ${json.encodeToString(request)}")

            // Request de autenticación (no requiere addAuth)
            val response: HttpResponse = client.post("/api/authenticate") {
                setBody(request)
            }

            println("Status code: ${response.status.value}")

            if (response.status.value in 200..299) {
                // Deserializa respuesta JSON → LoginResponseDTO
                val loginResponseDTO: LoginResponseDTO = response.body()

                // Persistimos el token en memoria
                setTokens(loginResponseDTO.id_token)

                println("✅ Login exitoso!")
                Result.success(loginResponseDTO.id_token)
            } else {
                // [ERROR] Leemos el body crudo para mostrar el motivo del error
                val errorBody = response.bodyAsText()
                println("❌ Error: ${response.status} - $errorBody")
                Result.failure(Exception("Error ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            // [EXCEPTION] Problema de red, parseo, timeout, etc.
            println("💥 Excepción login: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Backend endpoint: GET /api/eventos/resumidos
     *
     * Función:
     * - Obtiene la lista de eventos visibles para el usuario (EventoResumenDTO).
     * - El backend puede resolver esto desde BD local o proxy.
     */
    suspend fun getEvents(): Result<List<EventoResumenDTO>> {
        return try {
            println("GET /api/eventos/resumidos")

            val response: HttpResponse = client.get("/api/eventos/resumidos") {
                addAuth() // Bearer token
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

    /**
     * Backend endpoint: GET /api/eventos/{externalId}/asientos
     *
     * Función:
     * - Solicita al BACKEND el mapa FINAL de asientos.
     * - El backend:
     *     - consulta Redis vía proxy,
     *     - completa asientos LIBRES por diferencia,
     *     - devuelve la grilla completa para UI (List<AsientoEstadoDTO>).
     *
     * Importante:
     * - eventoId es externalId (ID de cátedra), como se definio en el backend.
     * - Requiere JWT (addAuth).
     */
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

    /**
     * Backend endpoint: POST /api/eventos/{externalId}/bloqueos
     * - Requiere JWT.
     * - Construye AsientoBloqueoRequestDTO con eventoId + lista de ubicaciones.
     * - Devuelve AsientoBloqueoResponseDTO con resultado true/false y detalle.
     *
     * Función:
     * - Solicita al BACKEND bloquear asientos.
     * - El backend:
     *     - valida evento y rangos,
     *     - pre-chequea estado en Redis,
     *     - delega bloqueo real al proxy/cátedra.
     */
    suspend fun bloquearAsientos(
        eventoId: Long,
        asientos: List<AsientoUbicacionDTO>
    ): Result<AsientoBloqueoResponseDTO> {
        return try {
            println("POST /api/eventos/$eventoId/bloqueos")
            println("Request body (asientos): ${json.encodeToString(asientos)}")

            // [DTO] Body real que espera el backend: { eventoId, asientos:[{fila,columna},...] }
            val request = AsientoBloqueoRequestDTO(
                eventoId = eventoId,
                // [SANITIZE] Se mapea a ubicaciones limpias (fila/columna) por seguridad
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

    /**
     * Backend endpoint: POST /api/ventas/eventos/{externalId}/venta
     * - Requiere JWT.
     * - request incluye asientos + persona (según tu backend).
     * - Devuelve VentaResponseDTO con resultado/descripcion/etc.
     *
     * Función:
     * - Solicita al BACKEND confirmar una venta.
     * - El backend:
     *     - valida bloqueos vigentes,
     *     - confirma venta con la cátedra vía proxy,
     *     - persiste venta y asientos vendidos localmente.
     */
    suspend fun venderAsientos(eventoId: Long, request: VentaRequestDTO): Result<VentaResponseDTO> {
        return try {
            println("POST /api/ventas/eventos/$eventoId/venta")
            println("Request body: ${json.encodeToString(request)}")

            val response: HttpResponse = client.post("/api/ventas/eventos/$eventoId/venta") {
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
