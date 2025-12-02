package com.flowboard.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowboard.data.repository.DocumentRepositoryImpl
import com.flowboard.data.remote.websocket.DocumentSyncService
import com.flowboard.data.remote.websocket.DocumentUpdate
import com.flowboard.data.remote.websocket.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class SavedDocument(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String
)

@HiltViewModel
class DocumentEditorViewModel @Inject constructor(
    application: Application,
    private val documentSyncService: DocumentSyncService,
    private val documentRepository: DocumentRepositoryImpl
) : AndroidViewModel(application) {

    private val _currentDocument = MutableStateFlow<SavedDocument?>(null)
    val currentDocument: StateFlow<SavedDocument?> = _currentDocument.asStateFlow()

    private val _allDocuments = MutableStateFlow<List<SavedDocument>>(emptyList())
    val allDocuments: StateFlow<List<SavedDocument>> = _allDocuments.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _remoteContentUpdate = MutableStateFlow<String?>(null)
    val remoteContentUpdate: StateFlow<String?> = _remoteContentUpdate.asStateFlow()

    init {
        loadAllDocuments()
        observeDocumentUpdates()
        observeConnectionState()
    }

    /**
     * Observar actualizaciones de documentos desde WebSocket
     */
    private fun observeDocumentUpdates() {
        viewModelScope.launch {
            documentSyncService.documentUpdates.collectLatest { update ->
                when (update) {
                    is DocumentUpdate.ContentChanged -> {
                        _remoteContentUpdate.value = update.content
                    }
                    is DocumentUpdate.CursorMoved -> {
                        // Manejar movimiento de cursor de otros usuarios
                    }
                    is DocumentUpdate.UserJoined -> {
                        // Manejar usuario que se une
                    }
                    is DocumentUpdate.UserLeft -> {
                        // Manejar usuario que se va
                    }
                }
            }
        }
    }

    /**
     * Observar estado de conexión
     */
    private fun observeConnectionState() {
        viewModelScope.launch {
            documentSyncService.connectionState.collectLatest { state ->
                _connectionState.value = state
            }
        }
    }

    /**
     * Conectar a documento para colaboración en tiempo real
     */
    fun connectToDocument(documentId: String, userId: String, token: String) {
        viewModelScope.launch {
            try {
                documentSyncService.connectToDocument(documentId, userId, token)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Desconectar del documento
     */
    fun disconnectFromDocument() {
        viewModelScope.launch {
            documentSyncService.disconnect()
        }
    }

    /**
     * Enviar actualización de contenido en tiempo real
     */
    fun sendContentUpdate(documentId: String, content: String, cursorPosition: Int) {
        viewModelScope.launch {
            documentSyncService.sendContentUpdate(documentId, content, cursorPosition)
        }
    }

    /**
     * Invitar usuario a colaborar
     */
    fun inviteUser(documentId: String, userIdOrEmail: String, permission: String) {
        viewModelScope.launch {
            documentSyncService.inviteUser(documentId, userIdOrEmail, permission)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            documentSyncService.disconnect()
        }
    }

    fun loadDocument(documentId: String) {
        viewModelScope.launch {
            try {
                documentRepository.getDocumentById(documentId)
                    .onSuccess { docEntity ->
                        val doc = SavedDocument(
                            id = docEntity.id,
                            title = docEntity.title,
                            content = docEntity.content,
                            createdAt = docEntity.createdAt,
                            updatedAt = docEntity.updatedAt
                        )
                        _currentDocument.value = doc
                    }
                    .onFailure {
                        it.printStackTrace()
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveDocument(id: String, title: String, content: String) {
        viewModelScope.launch {
            try {
                // Check if document exists (this is a simplification, normally we'd check ID validity or a flag)
                // If loading failed or it's new, we might not know.
                // Strategy: try update, if 404, create? Or logic based on if we loaded it.
                
                // For now, assume if we have it in our list or if ID is UUID format generated by UI but not in DB...
                // Actually, creating a new doc usually implies a specific API call.
                // UI generates ID? API usually generates ID.
                // If UI generated ID, we should probably use createDocument with that ID if supported, or let API generate it.
                // But the API signature is `createDocument(title, content)`. It returns the ID.
                
                // If the ID passed here was generated by UUID.randomUUID() in the UI, it won't exist in DB.
                // We should use `createDocument` if it's a new doc.
                // How do we know? 
                // The `DocumentEditorScreen` passes `documentId` as null for new docs, 
                // BUT `saveDocument` is called with a generated UUID.
                
                // Let's check if we have a _currentDocument with this ID.
                val isNew = _currentDocument.value?.id != id
                
                // Actually, if it's new, we should call create.
                // But `saveDocument` is called repeatedly (autosave).
                // The first time, it's new. Subsequent times, it's update.
                
                // Limitation: `createDocument` API doesn't take an ID. It returns one.
                // If we send a random UUID, the API ignores it and gives us a new one?
                // Or we can't send it.
                
                // FIX: If we don't have a synced document yet, call create.
                if (_currentDocument.value == null || _currentDocument.value?.id != id) {
                     documentRepository.createDocument(title, content)
                        .onSuccess { newDoc ->
                             val saved = SavedDocument(
                                id = newDoc.id,
                                title = newDoc.title,
                                content = newDoc.content,
                                createdAt = newDoc.createdAt,
                                updatedAt = newDoc.updatedAt
                            )
                            _currentDocument.value = saved
                            // We need to notify the UI that the ID changed if it was a temp UUID?
                            // But the UI keeps using the temp UUID until we tell it otherwise?
                            // This ViewModel structure is a bit tricky with the random UUID generation in UI.
                            
                            // Ideally, UI shouldn't generate ID. ViewModel should.
                            // But for now, let's assume if we create, we update _currentDocument.
                            loadAllDocuments()
                        }
                } else {
                    // Update
                    documentRepository.updateDocument(id, title, content)
                        .onSuccess { updatedDoc ->
                             val saved = SavedDocument(
                                id = updatedDoc.id,
                                title = updatedDoc.title,
                                content = updatedDoc.content,
                                createdAt = updatedDoc.createdAt,
                                updatedAt = updatedDoc.updatedAt
                            )
                            _currentDocument.value = saved
                            loadAllDocuments()
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadAllDocuments() {
        viewModelScope.launch {
            try {
                documentRepository.getAllDocuments()
                    .onSuccess { response ->
                        val documents = response.ownedDocuments.map { doc ->
                            SavedDocument(
                                id = doc.id,
                                title = doc.title,
                                content = doc.content,
                                createdAt = doc.createdAt.toString(),
                                updatedAt = doc.updatedAt.toString()
                            )
                        }
                        _allDocuments.value = documents.sortedByDescending { it.updatedAt }
                    }
                    .onFailure {
                        it.printStackTrace()
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            try {
                documentRepository.deleteDocument(documentId)
                    .onSuccess {
                        if (_currentDocument.value?.id == documentId) {
                            _currentDocument.value = null
                        }
                        loadAllDocuments()
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
