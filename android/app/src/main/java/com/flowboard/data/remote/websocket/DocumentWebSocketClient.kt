package com.flowboard.data.remote.websocket

import com.flowboard.data.models.crdt.CollaborativeDocument
import com.flowboard.data.models.crdt.DocumentOperation
import com.flowboard.data.remote.dto.UserPresenceInfo
// import com.tap.synk.Synk
// import com.tap.synk.adapter.SynkAdapter
// import com.tap.synk.models.Syncable
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentWebSocketClient @Inject constructor(
    private val client: HttpClient,
    private val json: Json
) {
    private val _webSocketState = MutableStateFlow<WebSocketState>(WebSocketState.Disconnected)
    val webSocketState: StateFlow<WebSocketState> = _webSocketState.asStateFlow()

    private val _operations = MutableSharedFlow<DocumentOperation>()
    val operations: SharedFlow<DocumentOperation> = _operations.asSharedFlow()

    private val _documentState = MutableSharedFlow<CollaborativeDocument>()
    val documentState: SharedFlow<CollaborativeDocument> = _documentState.asSharedFlow()


    private val _activeUsers = MutableStateFlow<List<UserPresenceInfo>>(emptyList())
    val activeUsers: StateFlow<List<UserPresenceInfo>> = _activeUsers.asStateFlow()

    private val _updatedBlock = MutableSharedFlow<com.flowboard.domain.model.ContentBlock>()
    val updatedBlock: SharedFlow<com.flowboard.domain.model.ContentBlock> = _updatedBlock.asSharedFlow()

    private var session: DefaultClientWebSocketSession? = null
    // private var synk: Synk? = null

    /*
    private fun getSynk(): Synk {
        if (synk == null) {
            val adapter = object : SynkAdapter<com.flowboard.domain.model.ContentBlock> {
                override fun resolveId(syncable: com.flowboard.domain.model.ContentBlock): String {
                    return syncable.id
                }

                override fun serialize(syncable: com.flowboard.domain.model.ContentBlock): String {
                    return json.encodeToString(com.flowboard.domain.model.ContentBlock.serializer(), syncable)
                }

                override fun deserialize(id: String, syncable: String): com.flowboard.domain.model.ContentBlock? {
                    return json.decodeFromString<com.flowboard.domain.model.ContentBlock>(syncable)
                }
            }
            synk = Synk.Builder(adapter).build()
        }
        return synk!!
    }
    */

    suspend fun connect(boardId: String, token: String) {
        if (session != null && session?.isActive == true) {
            return
        }
        _webSocketState.value = WebSocketState.Connecting
        try {
            client.webSocket(
                method = HttpMethod.Get,
                host = "localhost", // TODO: use real host
                port = 8080,
                path = "/ws/boards",
                request = {
                    header("Authorization", "Bearer $token")
                }
            ) {
                session = this
                _webSocketState.value = WebSocketState.Connected(boardId) // Modified to match WebSocketState.Connected(boardId: String)

                // Join room
                /*
                val joinMessage = com.flowboard.data.models.JoinRoomMessage(
                    timestamp = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.UTC),
                    boardId = boardId,
                    userId = "" // TODO: get real user id
                )
                send(Frame.Text(json.encodeToString(joinMessage)))
                */

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        // val message = json.decodeFromString<CrdtMessage>(text)
                        /*
                        when (message) {
                            is com.flowboard.data.models.crdt.DocumentStateMessage -> {
                                _documentState.emit(message.document)
                            }
                            is com.flowboard.data.models.crdt.DocumentOperationMessage -> {
                                _operations.emit(message.operation)
                            }
                            is com.flowboard.data.models.crdt.CursorUpdateMessage -> {
                                // TODO
                            }
                            is com.flowboard.data.models.SynkMessage -> {
                                val synk = getSynk()
                                val updatedBlock = synk.apply(message.message)
                                if (updatedBlock is com.flowboard.domain.model.ContentBlock) {
                                    _updatedBlock.emit(updatedBlock)
                                }
                            }
                        }
                        */
                    }
                }
            }
        } catch (e: Exception) {
            _webSocketState.value = WebSocketState.Error(e.message ?: "Unknown error")
        } finally {
            session = null
            _webSocketState.value = WebSocketState.Disconnected
        }
    }

    suspend fun sendOperation(operation: DocumentOperation) {
        /*
        val message = com.flowboard.data.models.crdt.DocumentOperationMessage(
            timestamp = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.UTC),
            operation = operation
        )
        session?.send(Frame.Text(json.encodeToString(message)))
        */
    }

    fun disconnect() {
        session?.cancel()
        session = null
    }
}