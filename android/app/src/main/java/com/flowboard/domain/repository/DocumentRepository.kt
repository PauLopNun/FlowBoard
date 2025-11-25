package com.flowboard.domain.repository

import com.flowboard.data.models.crdt.CollaborativeDocument
import com.flowboard.data.models.crdt.DocumentOperation
import com.flowboard.data.remote.dto.UserPresenceInfo
import com.flowboard.data.remote.websocket.WebSocketState
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    suspend fun getDocument(boardId: String): Flow<CollaborativeDocument>
    suspend fun sendOperation(operation: DocumentOperation)
    fun getConnectionState(): Flow<WebSocketState>
    val activeUsers: Flow<List<UserPresenceInfo>>
    fun getOperations(): Flow<DocumentOperation>
    fun getUpdatedBlock(): Flow<com.flowboard.domain.model.ContentBlock>
}
