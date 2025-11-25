package com.flowboard.data.remote.websocket

import android.util.Log
import com.flowboard.data.remote.dto.*
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cliente WebSocket para comunicación en tiempo real con el servidor
 *
 * Características:
 * - Reconexión automática con backoff exponencial
 * - Ping/Pong automático para mantener conexión activa
 * - Estados de conexión observables (StateFlow)
 * - Stream de mensajes entrantes (Flow)
 * - Manejo robusto de errores
 */
@Singleton
class TaskWebSocketClient @Inject constructor(
    private val client: HttpClient
) {
    companion object {
        private const val TAG = "TaskWebSocketClient"
        private const val WS_URL = "ws://10.0.2.2:8080/ws/boards" // Android emulator localhost
        private const val PING_INTERVAL_MS = 30_000L // 30 segundos
        private const val MAX_RECONNECT_ATTEMPTS = 10
        private const val INITIAL_RECONNECT_DELAY_MS = 1000L // 1 segundo
        private const val MAX_RECONNECT_DELAY_MS = 30_000L // 30 segundos
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Estado de conexión observable
    private val _connectionState = MutableStateFlow<WebSocketState>(WebSocketState.Disconnected)
    val connectionState: StateFlow<WebSocketState> = _connectionState.asStateFlow()

    // Canal para mensajes entrantes
    private val _incomingMessages = Channel<WebSocketMessage>(Channel.UNLIMITED)
    val incomingMessages: Flow<WebSocketMessage> = _incomingMessages.receiveAsFlow()

    // Sesión WebSocket activa
    private var webSocketSession: DefaultClientWebSocketSession? = null
    private var connectionScope: CoroutineScope? = null

    // JWT Token y User ID
    private var jwtToken: String? = null
    private var userId: String? = null
    private var currentBoardId: String? = null

    /**
     * Conecta al WebSocket y se une a un board específico
     */
    suspend fun connect(boardId: String, token: String, userIdParam: String) {
        if (_connectionState.value is WebSocketState.Connected) {
            Log.d(TAG, "Already connected, disconnecting first")
            disconnect()
        }

        jwtToken = token
        userId = userIdParam
        currentBoardId = boardId

        _connectionState.value = WebSocketState.Connecting

        connectWithRetry(boardId, token, userIdParam)
    }

    /**
     * Conecta con reintentos automáticos
     */
    private suspend fun connectWithRetry(boardId: String, token: String, userIdParam: String) {
        var attempt = 0
        var delay = INITIAL_RECONNECT_DELAY_MS

        while (attempt < MAX_RECONNECT_ATTEMPTS) {
            try {
                attempt++

                if (attempt > 1) {
                    _connectionState.value = WebSocketState.Reconnecting(attempt, MAX_RECONNECT_ATTEMPTS)
                    Log.d(TAG, "Reconnect attempt $attempt/$MAX_RECONNECT_ATTEMPTS (delay: ${delay}ms)")
                    delay(delay)
                }

                // Intentar establecer conexión
                establishConnection(boardId, token, userIdParam)

                // Si llegamos aquí, la conexión fue exitosa
                _connectionState.value = WebSocketState.Connected(boardId)
                Log.i(TAG, "Successfully connected to WebSocket")

                // Resetear contador de intentos
                attempt = 0
                delay = INITIAL_RECONNECT_DELAY_MS

                return // Salir del loop de reintentos

            } catch (e: CancellationException) {
                Log.d(TAG, "Connection cancelled")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Connection attempt $attempt failed: ${e.message}", e)

                if (attempt >= MAX_RECONNECT_ATTEMPTS) {
                    _connectionState.value = WebSocketState.Error(
                        message = "Failed to connect after $MAX_RECONNECT_ATTEMPTS attempts",
                        isRecoverable = true
                    )
                    return
                }

                // Backoff exponencial
                delay = (delay * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
            }
        }
    }

    /**
     * Establece la conexión WebSocket y maneja el ciclo de vida
     */
    private suspend fun establishConnection(boardId: String, token: String, userIdParam: String) {
        connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        client.webSocket(
            urlString = WS_URL,
            request = {
                headers.append("Authorization", "Bearer $token")
            }
        ) {
            webSocketSession = this

            try {
                // Enviar JOIN_ROOM inmediatamente
                sendJoinRoom(boardId, userIdParam)

                // Iniciar ping automático
                val pingJob = connectionScope?.launch {
                    startPingLoop()
                }

                // Escuchar mensajes entrantes
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            handleIncomingMessage(text)
                        }
                        is Frame.Close -> {
                            Log.d(TAG, "Server closed connection: ${closeReason.await()}")
                            break
                        }
                        else -> { /* Ignorar otros tipos de frames */ }
                    }
                }

                pingJob?.cancel()

            } catch (e: ClosedReceiveChannelException) {
                Log.d(TAG, "Connection closed normally")
            } catch (e: Exception) {
                Log.e(TAG, "Error in WebSocket session: ${e.message}", e)
                throw e
            } finally {
                webSocketSession = null
                _connectionState.value = WebSocketState.Disconnected

                // Intentar reconectar automáticamente
                if (currentBoardId != null && jwtToken != null && userId != null) {
                    Log.d(TAG, "Connection lost, attempting to reconnect...")
                    connectWithRetry(currentBoardId!!, jwtToken!!, userId!!)
                }
            }
        }
    }

    /**
     * Maneja mensajes entrantes y los emite al Flow
     */
    private suspend fun handleIncomingMessage(text: String) {
        try {
            Log.d(TAG, "Received message: $text")

            // Parsear tipo de mensaje
            val typeMap = json.decodeFromString<Map<String, String>>(text)
            val messageType = typeMap["type"]

            val message: WebSocketMessage? = when (messageType) {
                "ROOM_JOINED" -> json.decodeFromString<RoomJoinedMessage>(text)
                "TASK_CREATED" -> json.decodeFromString<TaskCreatedMessage>(text)
                "TASK_UPDATED" -> json.decodeFromString<TaskUpdatedMessage>(text)
                "TASK_DELETED" -> json.decodeFromString<TaskDeletedMessage>(text)
                "TASK_MOVED" -> json.decodeFromString<TaskMovedMessage>(text)
                "USER_JOINED" -> json.decodeFromString<UserJoinedMessage>(text)
                "USER_LEFT" -> json.decodeFromString<UserLeftMessage>(text)
                "USER_TYPING" -> json.decodeFromString<UserTypingMessage>(text)
                "PONG" -> json.decodeFromString<PongMessage>(text)
                "ERROR" -> json.decodeFromString<ErrorMessage>(text)
                else -> {
                    Log.w(TAG, "Unknown message type: $messageType")
                    null
                }
            }

            message?.let { _incomingMessages.send(it) }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message: $text", e)
        }
    }

    /**
     * Envía un mensaje JOIN_ROOM al servidor
     */
    private suspend fun sendJoinRoom(boardId: String, userIdParam: String) {
        val message = JoinRoomMessage(
            timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            boardId = boardId,
            userId = userIdParam
        )
        send(message)
        Log.d(TAG, "Sent JOIN_ROOM for board $boardId")
    }

    /**
     * Envía un mensaje genérico
     */
    suspend fun send(message: WebSocketMessage) {
        try {
            val jsonText = json.encodeToString(message)
            webSocketSession?.send(Frame.Text(jsonText))
            Log.d(TAG, "Sent message: ${message.type}")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}", e)
        }
    }

    /**
     * Envía indicador de escritura
     */
    suspend fun sendTypingIndicator(boardId: String, taskId: String?, isTyping: Boolean) {
        if (userId == null) return

        val message = TypingIndicatorMessage(
            timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            boardId = boardId,
            taskId = taskId,
            userId = userId!!,
            isTyping = isTyping
        )
        send(message)
    }

    /**
     * Loop de ping para mantener conexión activa
     */
    private suspend fun startPingLoop() {
        while (webSocketSession?.isActive == true) {
            delay(PING_INTERVAL_MS)
            try {
                val ping = PingMessage(timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
                send(ping)
                Log.d(TAG, "Sent PING")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending ping: ${e.message}", e)
                break
            }
        }
    }

    /**
     * Desconecta del WebSocket
     */
    suspend fun disconnect() {
        try {
            currentBoardId?.let { boardId ->
                val leaveMessage = LeaveRoomMessage(
                    timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                    boardId = boardId
                )
                send(leaveMessage)
            }

            webSocketSession?.close(CloseReason(CloseReason.Codes.NORMAL, "Client disconnect"))
            webSocketSession = null
            connectionScope?.cancel()
            connectionScope = null

            _connectionState.value = WebSocketState.Disconnected

            // Limpiar estado
            jwtToken = null
            userId = null
            currentBoardId = null

            Log.d(TAG, "Disconnected from WebSocket")

        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting: ${e.message}", e)
        }
    }

    /**
     * Reconecta manualmente
     */
    suspend fun reconnect() {
        val boardId = currentBoardId
        val token = jwtToken
        val userIdParam = userId

        if (boardId != null && token != null && userIdParam != null) {
            disconnect()
            connect(boardId, token, userIdParam)
        } else {
            Log.w(TAG, "Cannot reconnect: missing connection parameters")
        }
    }
}
