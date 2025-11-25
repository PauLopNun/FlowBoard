package com.flowboard.data.remote.websocket

/**
 * Estados de conexión WebSocket
 */
sealed class WebSocketState {
    /**
     * Desconectado, no hay conexión activa
     */
    object Disconnected : WebSocketState()

    /**
     * Intentando establecer conexión inicial
     */
    object Connecting : WebSocketState()

    /**
     * Conectado y listo para enviar/recibir mensajes
     */
    data class Connected(val boardId: String) : WebSocketState()

    /**
     * Intentando reconectar después de perder conexión
     */
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : WebSocketState()

    /**
     * Error fatal que impide la conexión
     */
    data class Error(val message: String, val isRecoverable: Boolean = true) : WebSocketState()
}
