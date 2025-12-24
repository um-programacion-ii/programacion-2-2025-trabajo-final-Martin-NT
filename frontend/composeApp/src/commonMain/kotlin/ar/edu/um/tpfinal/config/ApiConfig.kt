package ar.edu.um.tpfinal.config

import ar.edu.um.tpfinal.getPlatform

/**
 * Objeto de configuración central del frontend.
 * Define la URL base a la que el frontend va a hacer las requests HTTP.
 */
object ApiConfig {

    val baseUrl: String
        get() {
            val platform = getPlatform().name
            val url = when {
                platform.startsWith("Android") -> "http://10.0.2.2:8080"
                platform.startsWith("iOS") -> "http://localhost:8080"
                else -> "http://localhost:8080"
            }
            println("🌐 ApiConfig → platform=$platform | baseUrl=$url")
            return url

        }
}