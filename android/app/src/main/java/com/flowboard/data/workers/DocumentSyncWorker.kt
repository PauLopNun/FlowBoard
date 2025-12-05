package com.flowboard.data.workers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flowboard.data.local.dao.DocumentDao
import com.flowboard.data.local.dao.PendingOperationDao
import com.flowboard.data.local.entities.EntityType
import com.flowboard.data.local.entities.OperationType
import com.flowboard.data.local.entities.PendingOperationEntity
import com.flowboard.data.remote.api.DocumentApiService
import com.flowboard.data.repository.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Worker para sincronizar documentos en segundo plano
 *
 * Este worker:
 * 1. Verifica conectividad a internet
 * 2. Verifica autenticación del usuario
 * 3. SINCRONIZACIÓN BIDIRECCIONAL:
 *    a) Sube cambios locales al servidor (operaciones pendientes)
 *    b) Descarga cambios del servidor a local
 * 4. Maneja conflictos con estrategia last-write-wins
 * 5. Actualiza el estado de sincronización
 */
@HiltWorker
class DocumentSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val documentDao: DocumentDao,
    private val pendingOperationDao: PendingOperationDao,
    private val documentApiService: DocumentApiService,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "DocumentSyncWorker"
        const val DOCUMENT_ID_KEY = "documentId"
        const val SYNC_MODE_KEY = "syncMode"
        const val SYNC_MODE_UPLOAD = "upload"
        const val SYNC_MODE_DOWNLOAD = "download"
        const val SYNC_MODE_BIDIRECTIONAL = "bidirectional"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting document sync worker")

        // 1. Verificar conectividad
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No network available, retrying later")
            return Result.retry()
        }

        // 2. Verificar autenticación
        val token = authRepository.getToken()
        if (token == null) {
            Log.d(TAG, "User not authenticated, retrying later")
            return Result.retry()
        }

        // 3. Determinar modo de sincronización
        val syncMode = inputData.getString(SYNC_MODE_KEY) ?: SYNC_MODE_BIDIRECTIONAL
        val documentId = inputData.getString(DOCUMENT_ID_KEY)

        return try {
            when (syncMode) {
                SYNC_MODE_UPLOAD -> {
                    // Solo subir cambios locales
                    if (documentId != null) {
                        uploadDocument(documentId)
                    } else {
                        uploadAllPendingChanges()
                    }
                }
                SYNC_MODE_DOWNLOAD -> {
                    // Solo descargar cambios del servidor
                    downloadServerChanges()
                }
                SYNC_MODE_BIDIRECTIONAL -> {
                    // Sincronización completa bidireccional
                    if (documentId != null) {
                        // Subir documento específico y luego descargar cambios
                        uploadDocument(documentId)
                        downloadServerChanges()
                    } else {
                        // Primero subir todos los cambios locales
                        uploadAllPendingChanges()
                        // Luego descargar cambios del servidor
                        downloadServerChanges()
                    }
                }
            }

            Log.d(TAG, "Sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            e.printStackTrace()

            // Reintentar si hay error
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    /**
     * SUBIR: Procesar todas las operaciones pendientes y subirlas al servidor
     */
    private suspend fun uploadAllPendingChanges() {
        Log.d(TAG, "Uploading pending changes to server")

        // Obtener operaciones pendientes que no han fallado demasiadas veces
        val pendingOperations = pendingOperationDao.getRetryableOperations(maxAttempts = 5)

        Log.d(TAG, "Found ${pendingOperations.size} pending operations")

        pendingOperations.forEach { operation ->
            try {
                processPendingOperation(operation)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process operation ${operation.id}", e)
                // Incrementar contador de intentos
                val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
                pendingOperationDao.incrementAttempts(
                    id = operation.id,
                    attemptTime = now,
                    error = e.message
                )
            }
        }
    }

    /**
     * Procesar una operación pendiente específica
     */
    private suspend fun processPendingOperation(operation: PendingOperationEntity) {
        Log.d(TAG, "Processing ${operation.operationType} operation for ${operation.entityType} ${operation.entityId}")

        when (operation.entityType) {
            EntityType.DOCUMENT -> {
                when (operation.operationType) {
                    OperationType.CREATE -> createDocumentOnServer(operation)
                    OperationType.UPDATE -> updateDocumentOnServer(operation)
                    OperationType.DELETE -> deleteDocumentOnServer(operation)
                }
            }
            // Agregar otros tipos de entidades aquí en el futuro
        }

        // Si llegamos aquí, la operación fue exitosa
        pendingOperationDao.deleteOperation(operation)
        Log.d(TAG, "Operation ${operation.id} completed successfully")
    }

    /**
     * Crear documento en el servidor
     */
    private suspend fun createDocumentOnServer(operation: PendingOperationEntity) {
        val localDoc = documentDao.getDocumentById(operation.entityId) ?: return

        val remoteDoc = documentApiService.createDocument(
            title = localDoc.title,
            content = localDoc.content,
            isPublic = localDoc.isPublic
        )

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()

        // Si el servidor asignó un ID diferente, actualizar el documento local
        if (remoteDoc.id != localDoc.id) {
            documentDao.deleteDocumentById(localDoc.id)
            documentDao.insertDocument(localDoc.copy(
                id = remoteDoc.id,
                isSync = true,
                lastSyncAt = now
            ))
        } else {
            documentDao.updateSyncStatus(localDoc.id, isSync = true, lastSyncAt = now)
        }
    }

    /**
     * Actualizar documento en el servidor
     */
    private suspend fun updateDocumentOnServer(operation: PendingOperationEntity) {
        val localDoc = documentDao.getDocumentById(operation.entityId) ?: return

        val remoteDoc = documentApiService.updateDocument(
            id = localDoc.id,
            title = localDoc.title,
            content = localDoc.content,
            isPublic = localDoc.isPublic
        )

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()

        // Actualizar estado de sincronización
        documentDao.updateSyncStatus(localDoc.id, isSync = true, lastSyncAt = now)
    }

    /**
     * Eliminar documento en el servidor
     */
    private suspend fun deleteDocumentOnServer(operation: PendingOperationEntity) {
        try {
            documentApiService.deleteDocument(operation.entityId)
        } catch (e: Exception) {
            // Si el documento no existe en el servidor (404), considerarlo exitoso
            if (e.message?.contains("404") == true) {
                Log.d(TAG, "Document already deleted on server")
            } else {
                throw e
            }
        }

        // Eliminar de la base de datos local también
        documentDao.deleteDocumentById(operation.entityId)
    }

    /**
     * SUBIR: Subir un documento específico
     */
    private suspend fun uploadDocument(documentId: String) {
        Log.d(TAG, "Uploading document $documentId")

        // Buscar operaciones pendientes para este documento
        val operations = pendingOperationDao.getPendingOperationsForEntity(
            EntityType.DOCUMENT,
            documentId
        )

        operations.forEach { operation ->
            try {
                processPendingOperation(operation)
            } catch (e: Exception) {
                val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
                pendingOperationDao.incrementAttempts(
                    id = operation.id,
                    attemptTime = now,
                    error = e.message
                )
                throw e
            }
        }
    }

    /**
     * DESCARGAR: Obtener cambios del servidor y actualizar local
     * Implementa estrategia last-write-wins para conflictos
     */
    private suspend fun downloadServerChanges() {
        Log.d(TAG, "Downloading server changes")

        try {
            // Obtener todos los documentos del servidor
            val response = documentApiService.getAllDocuments()
            val serverDocuments = response.ownedDocuments + response.sharedWithMe

            Log.d(TAG, "Found ${serverDocuments.size} documents on server")

            serverDocuments.forEach { serverDoc ->
                try {
                    val localDoc = documentDao.getDocumentById(serverDoc.id)

                    if (localDoc == null) {
                        // Documento nuevo en el servidor, insertarlo localmente
                        Log.d(TAG, "New document from server: ${serverDoc.id}")
                        documentDao.insertDocument(serverDoc.toEntity())
                    } else {
                        // Documento existe localmente, verificar si necesita actualizarse
                        resolveConflict(localDoc, serverDoc.toEntity())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync document ${serverDoc.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download server changes", e)
            throw e
        }
    }

    /**
     * Resolver conflictos usando estrategia last-write-wins
     */
    private suspend fun resolveConflict(
        localDoc: com.flowboard.data.local.entities.DocumentEntity,
        serverDoc: com.flowboard.data.local.entities.DocumentEntity
    ) {
        // Si el documento local no está sincronizado, tiene prioridad
        if (!localDoc.isSync) {
            Log.d(TAG, "Local document ${localDoc.id} has pending changes, keeping local version")
            return
        }

        // Comparar timestamps para determinar cuál es más reciente
        val localUpdatedAt = try {
            LocalDateTime.parse(localDoc.updatedAt)
        } catch (e: Exception) {
            null
        }

        val serverUpdatedAt = try {
            LocalDateTime.parse(serverDoc.updatedAt)
        } catch (e: Exception) {
            null
        }

        if (localUpdatedAt != null && serverUpdatedAt != null) {
            if (serverUpdatedAt > localUpdatedAt) {
                // Servidor tiene versión más reciente
                Log.d(TAG, "Server has newer version of ${localDoc.id}, updating local")
                documentDao.updateDocument(serverDoc.copy(
                    isSync = true,
                    lastSyncAt = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
                ))
            } else {
                Log.d(TAG, "Local version of ${localDoc.id} is up to date")
            }
        } else {
            // Si no se pueden comparar fechas, usar versión del servidor
            Log.d(TAG, "Cannot compare timestamps, using server version for ${localDoc.id}")
            documentDao.updateDocument(serverDoc.copy(
                isSync = true,
                lastSyncAt = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
            ))
        }
    }

    /**
     * Verifica si hay conexión a internet
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
