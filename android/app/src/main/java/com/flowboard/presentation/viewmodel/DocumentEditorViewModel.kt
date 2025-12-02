package com.flowboard.presentation.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.flowboard.data.remote.websocket.DocumentSyncService
import com.flowboard.data.remote.websocket.DocumentUpdate
import com.flowboard.data.remote.websocket.ConnectionState
import kotlinx.coroutines.flow.collectLatest
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
    private val documentSyncService: DocumentSyncService
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("documents", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

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
                val docJson = prefs.getString("doc_$documentId", null)
                if (docJson != null) {
                    val doc = json.decodeFromString<SavedDocument>(docJson)
                    _currentDocument.value = doc
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveDocument(id: String, title: String, content: String) {
        viewModelScope.launch {
            try {
                val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()

                val document = SavedDocument(
                    id = id,
                    title = title,
                    content = content,
                    createdAt = _currentDocument.value?.createdAt ?: now,
                    updatedAt = now
                )

                // Guardar en SharedPreferences
                prefs.edit()
                    .putString("doc_$id", json.encodeToString(document))
                    .apply()

                // Actualizar lista de IDs
                val allIds = prefs.getStringSet("all_doc_ids", mutableSetOf()) ?: mutableSetOf()
                if (!allIds.contains(id)) {
                    prefs.edit()
                        .putStringSet("all_doc_ids", allIds.toMutableSet().apply { add(id) })
                        .apply()
                }

                _currentDocument.value = document

                // Recargar lista
                loadAllDocuments()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadAllDocuments() {
        viewModelScope.launch {
            try {
                val allIds = prefs.getStringSet("all_doc_ids", mutableSetOf()) ?: mutableSetOf()
                val documents = allIds.mapNotNull { id ->
                    val docJson = prefs.getString("doc_$id", null)
                    docJson?.let { json.decodeFromString<SavedDocument>(it) }
                }
                _allDocuments.value = documents.sortedByDescending { it.updatedAt }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            try {
                // Eliminar de SharedPreferences
                prefs.edit().remove("doc_$documentId").apply()

                // Actualizar lista de IDs
                val allIds = prefs.getStringSet("all_doc_ids", mutableSetOf()) ?: mutableSetOf()
                prefs.edit()
                    .putStringSet("all_doc_ids", allIds.toMutableSet().apply { remove(documentId) })
                    .apply()

                if (_currentDocument.value?.id == documentId) {
                    _currentDocument.value = null
                }

                // Recargar lista
                loadAllDocuments()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
