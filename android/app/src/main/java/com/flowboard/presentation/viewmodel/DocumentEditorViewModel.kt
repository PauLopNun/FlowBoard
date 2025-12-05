package com.flowboard.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.flowboard.data.local.dao.DocumentDao
import com.flowboard.data.local.dao.PendingOperationDao
import com.flowboard.data.local.entities.DocumentEntity
import com.flowboard.data.local.entities.EntityType
import com.flowboard.data.local.entities.OperationType
import com.flowboard.data.local.entities.PendingOperationEntity
import com.flowboard.data.repository.AuthRepository
import com.flowboard.data.repository.DocumentRepositoryImpl
import com.flowboard.data.remote.websocket.DocumentSyncService
import com.flowboard.data.remote.websocket.DocumentUpdate
import com.flowboard.data.remote.websocket.ConnectionState
import com.flowboard.data.workers.DocumentSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject

@Serializable
data class SavedDocument(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String,
    val isSync: Boolean = false,
    val lastSyncAt: String? = null
)

enum class SaveStatus {
    Idle,
    Saving,
    Saved,
    Error
}

@HiltViewModel
class DocumentEditorViewModel @Inject constructor(
    application: Application,
    private val documentDao: DocumentDao,
    private val pendingOperationDao: PendingOperationDao,
    private val authRepository: AuthRepository,
    private val documentSyncService: DocumentSyncService,
    private val documentRepository: DocumentRepositoryImpl
) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)

    private val _currentDocument = MutableStateFlow<SavedDocument?>(null)
    val currentDocument: StateFlow<SavedDocument?> = _currentDocument.asStateFlow()

    private val _allDocuments = MutableStateFlow<List<SavedDocument>>(emptyList())
    val allDocuments: StateFlow<List<SavedDocument>> = _allDocuments.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _remoteContentUpdate = MutableStateFlow<String?>(null)
    val remoteContentUpdate: StateFlow<String?> = _remoteContentUpdate.asStateFlow()

    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

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

    /**
     * Cargar documento por ID (desde local DB primero)
     */
    fun loadDocument(documentId: String) {
        viewModelScope.launch {
            try {
                // Primero intentar cargar desde DB local
                val localDoc = documentDao.getDocumentById(documentId)
                if (localDoc != null) {
                    _currentDocument.value = SavedDocument(
                        id = localDoc.id,
                        title = localDoc.title,
                        content = localDoc.content,
                        createdAt = localDoc.createdAt,
                        updatedAt = localDoc.updatedAt,
                        isSync = localDoc.isSync,
                        lastSyncAt = localDoc.lastSyncAt
                    )
                } else {
                    // Si no está en local, cargar desde servidor
                    documentRepository.getDocumentById(documentId)
                        .onSuccess { docEntity ->
                            val doc = SavedDocument(
                                id = docEntity.id,
                                title = docEntity.title,
                                content = docEntity.content,
                                createdAt = docEntity.createdAt,
                                updatedAt = docEntity.updatedAt,
                                isSync = true,
                                lastSyncAt = docEntity.lastSyncAt
                            )
                            _currentDocument.value = doc

                            // Guardar en local
                            documentDao.insertDocument(docEntity)
                        }
                        .onFailure {
                            it.printStackTrace()
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * GUARDADO OFFLINE-FIRST
     * Guarda primero en local, luego sincroniza en background
     */
    fun saveDocument(id: String?, title: String, content: String, isManualSave: Boolean = false) {
        viewModelScope.launch {
            try {
                if (isManualSave || _saveStatus.value != SaveStatus.Saving) {
                    _saveStatus.value = SaveStatus.Saving
                    _saveError.value = null
                }

                val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
                val userId = authRepository.getUserId() ?: "offline-user"

                // Determinar el ID del documento
                val documentId = id ?: _currentDocument.value?.id ?: UUID.randomUUID().toString()

                // Verificar si el documento ya existe localmente
                val existingDoc = documentDao.getDocumentById(documentId)
                val isNewDocument = existingDoc == null

                // 1. GUARDAR LOCAL PRIMERO (siempre funciona, incluso offline)
                val localDoc = DocumentEntity(
                    id = documentId,
                    title = title,
                    content = content,
                    ownerId = userId,
                    ownerName = null,
                    isPublic = false,
                    createdAt = existingDoc?.createdAt ?: now,
                    updatedAt = now,
                    lastEditedBy = userId,
                    lastEditedByName = null,
                    isSync = false,  // Marcar como no sincronizado
                    lastSyncAt = null
                )

                documentDao.insertDocument(localDoc)

                // 2. REGISTRAR OPERACIÓN PENDIENTE
                val operationType = if (isNewDocument) OperationType.CREATE else OperationType.UPDATE

                val pendingOperation = PendingOperationEntity(
                    operationType = operationType,
                    entityType = EntityType.DOCUMENT,
                    entityId = documentId,
                    data = null,  // Los datos están en la tabla documents
                    createdAt = now,
                    attempts = 0,
                    lastAttemptAt = null,
                    lastError = null
                )

                pendingOperationDao.insertOperation(pendingOperation)

                // Actualizar estado local
                _currentDocument.value = SavedDocument(
                    id = localDoc.id,
                    title = localDoc.title,
                    content = localDoc.content,
                    createdAt = localDoc.createdAt,
                    updatedAt = localDoc.updatedAt,
                    isSync = false,
                    lastSyncAt = null
                )

                _saveStatus.value = SaveStatus.Saved

                // 3. PROGRAMAR SINCRONIZACIÓN EN BACKGROUND
                scheduleSyncWork(documentId)

            } catch (e: Exception) {
                _saveStatus.value = SaveStatus.Error
                _saveError.value = e.message ?: "Unknown error"
                e.printStackTrace()
            }
        }
    }

    /**
     * Programar sincronización en background con WorkManager
     */
    private fun scheduleSyncWork(documentId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<DocumentSyncWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    DocumentSyncWorker.DOCUMENT_ID_KEY to documentId
                )
            )
            .addTag("document_sync_$documentId")
            .build()

        workManager.enqueue(syncWorkRequest)
    }

    /**
     * Resetear el estado de guardado (útil para limpiar el mensaje "Saved")
     */
    fun resetSaveStatus() {
        _saveStatus.value = SaveStatus.Idle
    }

    /**
     * Cargar todos los documentos (desde local DB)
     */
    fun loadAllDocuments() {
        viewModelScope.launch {
            try {
                documentDao.getAllDocuments().collect { documents ->
                    _allDocuments.value = documents.map { doc ->
                        SavedDocument(
                            id = doc.id,
                            title = doc.title,
                            content = doc.content,
                            createdAt = doc.createdAt,
                            updatedAt = doc.updatedAt,
                            isSync = doc.isSync,
                            lastSyncAt = doc.lastSyncAt
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Eliminar documento (OFFLINE-FIRST)
     * Marca para eliminar localmente y programa sincronización
     */
    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            try {
                val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()

                // 1. Registrar operación de eliminación pendiente ANTES de eliminar
                val pendingOperation = PendingOperationEntity(
                    operationType = OperationType.DELETE,
                    entityType = EntityType.DOCUMENT,
                    entityId = documentId,
                    data = null,
                    createdAt = now,
                    attempts = 0,
                    lastAttemptAt = null,
                    lastError = null
                )

                pendingOperationDao.insertOperation(pendingOperation)

                // 2. Eliminar de local (esto se hace después para que el worker pueda verificar si era sincronizado)
                // Nota: El worker eliminará tanto local como remoto cuando procese la operación

                // 3. Programar sincronización
                val syncWorkRequest = OneTimeWorkRequestBuilder<DocumentSyncWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .setInputData(
                        workDataOf(
                            DocumentSyncWorker.DOCUMENT_ID_KEY to documentId
                        )
                    )
                    .addTag("document_delete_$documentId")
                    .build()

                workManager.enqueue(syncWorkRequest)

                // Actualizar UI
                if (_currentDocument.value?.id == documentId) {
                    _currentDocument.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Forzar sincronización de todos los documentos pendientes
     */
    fun syncAllDocuments() {
        val syncWorkRequest = OneTimeWorkRequestBuilder<DocumentSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("sync_all_documents")
            .build()

        workManager.enqueue(syncWorkRequest)
    }
}
