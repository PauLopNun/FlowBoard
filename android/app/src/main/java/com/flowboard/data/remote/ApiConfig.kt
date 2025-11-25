package com.flowboard.data.remote

import com.flowboard.BuildConfig

/**
 * Configuración centralizada de URLs de la API
 *
 * En modo DEBUG: usa el emulador local (10.0.2.2:8080)
 * En modo RELEASE: usa el servidor de producción en Render
 */
object ApiConfig {
    // TODO: Reemplaza con tu URL de Render después del despliegue
    private const val PRODUCTION_BASE_URL = "https://flowboard-api.onrender.com"
    private const val DEVELOPMENT_BASE_URL = "http://10.0.2.2:8080"

    /**
     * URL base de la API según el tipo de build
     */
    val BASE_URL: String = if (BuildConfig.DEBUG) {
        DEVELOPMENT_BASE_URL
    } else {
        PRODUCTION_BASE_URL
    }

    /**
     * URL base de la API (HTTP)
     */
    val API_BASE_URL = "$BASE_URL/api/v1"

    /**
     * URL del WebSocket (WS/WSS)
     */
    val WS_BASE_URL: String = if (BuildConfig.DEBUG) {
        "ws://10.0.2.2:8080"
    } else {
        "wss://flowboard-api.onrender.com"
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
