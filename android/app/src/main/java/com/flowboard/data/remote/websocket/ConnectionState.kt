package com.flowboard.data.remote.websocket

/**
 * Estados de conexión del WebSocket
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

