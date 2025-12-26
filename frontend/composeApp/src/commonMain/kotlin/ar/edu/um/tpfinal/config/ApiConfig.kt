package ar.edu.um.tpfinal.config
import ar.edu.um.tpfinal.getPlatform
/**
 * Objeto de configuración central del FRONTEND.
 *
 * Rol:
 * - Define la URL base del BACKEND al que el frontend hará requests HTTP.
 * - Permite adaptar automáticamente la baseUrl según la plataforma (KMP).
 *
 * Importante:
 * - El frontend SIEMPRE llama al BACKEND (nunca al proxy).
 * - El proxy queda oculto detrás del backend.
 */
object ApiConfig {

    /**
     * Se resuelve según la plataforma:
     * - Android (Emulador): 10.0.2.2 → alias al localhost de la PC host
     * - iOS / Desktop: localhost
     *
     * Esto evita hardcodear URLs distintas por plataforma
     * y permite reutilizar el mismo ApiClient en KMP.
     */
    val baseUrl: String
        get() {
            // Detecta la plataforma actual (Android / iOS / Desktop)
            val platform = getPlatform().name

            // RESOLUCIÓN DE URL
            val url = when {
                platform.startsWith("Android") ->
                    // Android Emulator: localhost del host
                    "http://10.0.2.2:8080"

                platform.startsWith("iOS") ->
                    // iOS Simulator / local
                    "http://localhost:8080"

                else ->
                    // Desktop / fallback
                    "http://localhost:8080"
            }

            // [LOG] Útil para debug y validación de entorno
            println("🌐 ApiConfig → platform=$platform | baseUrl=$url")

            return url
        }
}
