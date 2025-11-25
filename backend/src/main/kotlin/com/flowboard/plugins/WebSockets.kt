package com.flowboard.plugins

import io.ktor.server.application.*
import io.ktor.server.websocket.*
import java.time.Duration

/**
 * Configura el plugin de WebSockets para Ktor
 */
fun Application.configureWebSockets() {
    install(WebSockets) {
        // Tiempo máximo antes de cerrar conexión inactiva
        pingPeriod = Duration.ofSeconds(30)

        // Timeout para respuesta de ping
        timeout = Duration.ofSeconds(15)

        // Tamaño máximo de frame (10 MB)
        maxFrameSize = Long.MAX_VALUE

        // Permite máscaras en frames (requerido por estándar WebSocket)
        masking = false

        // Extensiones de WebSocket
        extensions {
            // Se pueden agregar extensiones como deflate para compresión
        }
    }
}
