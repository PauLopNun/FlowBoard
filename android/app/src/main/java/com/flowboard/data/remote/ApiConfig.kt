package com.flowboard.data.remote

/**
 * Configuración centralizada de URLs de la API
 *
 * IMPORTANTE: Actualmente configurado para usar SIEMPRE el servidor de producción en Render.
 * Para desarrollo local, cambia USE_PRODUCTION a false.
 */
object ApiConfig {
    private const val PRODUCTION_BASE_URL = "https://flowboard-api-phrk.onrender.com"
    private const val DEVELOPMENT_BASE_URL = "http://10.0.2.2:8080"

    // Cambiar a false para usar backend local (10.0.2.2:8080)
    private const val USE_PRODUCTION = true

    /**
     * URL base de la API según la configuración
     */
    val BASE_URL: String = if (USE_PRODUCTION) {
        PRODUCTION_BASE_URL
    } else {
        DEVELOPMENT_BASE_URL
    }

    /**
     * URL base de la API (HTTP)
     */
    val API_BASE_URL = "$BASE_URL/api/v1"

    /**
     * URL del WebSocket (WS/WSS)
     */
    val WS_BASE_URL: String = if (USE_PRODUCTION) {
        "wss://flowboard-api-phrk.onrender.com"
    } else {
        "ws://10.0.2.2:8080"
    }

    /**
     * Endpoints principales
     */
    object Endpoints {
        const val AUTH = "/auth"
        const val TASKS = "/tasks"
        const val PROJECTS = "/projects"
        const val USERS = "/users"
        const val WS_BOARDS = "/ws/boards"
    }

    /**
     * Permite cambiar la URL de producción manualmente (útil para pruebas)
     */
    fun getCustomBaseUrl(customUrl: String): String {
        return if (customUrl.isNotEmpty()) customUrl else BASE_URL
    }
}
