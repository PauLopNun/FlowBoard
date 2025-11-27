package com.flowboard.data.repository

import com.flowboard.data.local.entities.DocumentEntity
import com.flowboard.data.models.crdt.CollaborativeDocument
import com.flowboard.data.models.crdt.DocumentOperation
import com.flowboard.data.remote.api.DocumentApiService
import com.flowboard.data.remote.dto.DocumentListResponse
import com.flowboard.data.remote.websocket.ConnectionState
import com.flowboard.data.remote.websocket.DocumentWebSocketClient
import com.flowboard.data.remote.websocket.WebSocketState
import com.flowboard.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

class DocumentRepositoryImpl @Inject constructor(
    private val webSocketClient: DocumentWebSocketClient,
    private val authRepository: AuthRepository,
    private val documentApiService: DocumentApiService
) : DocumentRepository {

    override suspend fun getDocument(boardId: String): Flow<CollaborativeDocument> {
        val token = authRepository.getToken()
        val userId = authRepository.getUserId()
        val userName = authRepository.getUserName()

        if (token != null && userId != null && userName != null) {
            webSocketClient.connect(
                documentId = boardId,
                userId = userId,
                userName = userName,
                token = token
            )
        }
        return webSocketClient.documentState.filterNotNull()
    }

    override suspend fun sendOperation(operation: DocumentOperation) {
        val userId = authRepository.getUserId()
        if (userId != null) {
            webSocketClient.sendOperation(operation, userId)
        }
    }

    override fun getConnectionState(): Flow<WebSocketState> {
        return webSocketClient.connectionState.map { state ->
            when (state) {
                is ConnectionState.Disconnected -> WebSocketState.Disconnected
                is ConnectionState.Connecting -> WebSocketState.Connecting
                is ConnectionState.Connected -> WebSocketState.Connected("")
                is ConnectionState.Error -> WebSocketState.Error(state.message)
            }
        }
    }

    override val activeUsers: Flow<List<com.flowboard.data.remote.dto.UserPresenceInfo>>
        get() = webSocketClient.activeUsers.map { users ->
            users.map { user ->
                com.flowboard.data.remote.dto.UserPresenceInfo(
                    userId = user.userId,
                    username = user.userName,
                    fullName = user.userName,
                    profileImageUrl = null,
                    isOnline = user.isOnline,
                    lastActivity = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                )
            }
        }

    override fun getOperations(): Flow<DocumentOperation> {
        return flow {
            webSocketClient.incomingOperations.collect { broadcast ->
                emit(broadcast.operation)
            }
        }
    }

    override fun getUpdatedBlock(): Flow<com.flowboard.domain.model.ContentBlock> {
        return flow {
            // This is not directly supported by the new WebSocket client
            // Would need to derive from document state changes
        }
    }

    // Document CRUD operations
    suspend fun getAllDocuments(): Result<DocumentListResponse> {
        return try {
            val response = documentApiService.getAllDocuments()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDocumentById(id: String): Result<DocumentEntity> {
        return try {
            val document = documentApiService.getDocumentById(id)
            Result.success(document)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDocument(title: String, content: String = "", isPublic: Boolean = false): Result<DocumentEntity> {
        return try {
            val document = documentApiService.createDocument(title, content, isPublic)
            Result.success(document)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDocument(id: String, title: String? = null, content: String? = null, isPublic: Boolean? = null): Result<DocumentEntity> {
        return try {
            val document = documentApiService.updateDocument(id, title, content, isPublic)
            Result.success(document)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(id: String): Result<Unit> {
        return try {
            documentApiService.deleteDocument(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun shareDocument(documentId: String, email: String, role: String): Result<Unit> {
        return try {
            documentApiService.shareDocument(documentId, email, role)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
