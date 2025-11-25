package com.flowboard.data.repository

import com.flowboard.data.models.crdt.CollaborativeDocument
import com.flowboard.data.models.crdt.DocumentOperation
import com.flowboard.data.remote.dto.UserPresenceInfo
import com.flowboard.data.remote.websocket.DocumentWebSocketClient
import com.flowboard.data.remote.websocket.WebSocketState
import com.flowboard.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DocumentRepositoryImpl @Inject constructor(
    private val webSocketClient: DocumentWebSocketClient,
    private val authRepository: AuthRepository
) : DocumentRepository {

    override suspend fun getDocument(boardId: String): Flow<CollaborativeDocument> {
        val token = authRepository.getToken()
        if (token != null) {
            webSocketClient.connect(boardId, token)
        }
        return webSocketClient.documentState
    }

    override suspend fun sendOperation(operation: DocumentOperation) {
        webSocketClient.sendOperation(operation)
    }

    override fun getConnectionState(): Flow<WebSocketState> {
        return webSocketClient.webSocketState
    }

    override val activeUsers: Flow<List<UserPresenceInfo>>
        get() = webSocketClient.activeUsers

    override fun getOperations(): Flow<DocumentOperation> {
        return webSocketClient.operations
    }

    override fun getUpdatedBlock(): Flow<com.flowboard.domain.model.ContentBlock> {
        return webSocketClient.updatedBlock
    }
}
