package com.flowboard.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.work.*
import com.flowboard.data.workers.DocumentSyncWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor centralizado de sincronización offline-first
 *
 * Responsabilidades:
 * 1. Programar sincronización periódica en background
 * 2. Detectar cambios de conectividad y sincronizar cuando se recupera la conexión
 * 3. Coordinar sincronizaciones manuales
 */
@Singleton
class SyncManager @Inject constructor(
    private val context: Context
) {
    companion object {
        const val TAG = "SyncManager"
        const val PERIODIC_SYNC_WORK_NAME = "periodic_document_sync"
        const val PERIODIC_SYNC_INTERVAL_MINUTES = 30L
    }

    private val workManager = WorkManager.getInstance(context)
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isNetworkCallbackRegistered = false

    /**
     * Inicializar el SyncManager
     * Debe llamarse cuando la app inicia
     */
    fun initialize() {
        Log.d(TAG, "Initializing SyncManager")

        // Programar sincronización periódica
        schedulePeriodicSync()

        // Registrar callback de red
        registerNetworkCallback()

        // Ejecutar sincronización inicial si hay conexión
        if (isNetworkAvailable()) {
            triggerImmediateSync()
        }
    }

    /**
     * Programar sincronización periódica
     */
    private fun schedulePeriodicSync() {
        Log.d(TAG, "Scheduling periodic sync every $PERIODIC_SYNC_INTERVAL_MINUTES minutes")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)  // No sincronizar si la batería está baja
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<DocumentSyncWorker>(
            PERIODIC_SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    DocumentSyncWorker.SYNC_MODE_KEY to DocumentSyncWorker.SYNC_MODE_BIDIRECTIONAL
                )
            )
            .addTag("periodic_sync")
            .build()

        // ExistingPeriodicWorkPolicy.KEEP mantiene el trabajo existente si ya está programado
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
    }

    /**
     * Registrar callback para detectar cambios de red
     */
    private fun registerNetworkCallback() {
        if (isNetworkCallbackRegistered) {
            Log.d(TAG, "Network callback already registered")
            return
        }

        Log.d(TAG, "Registering network callback")

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(TAG, "Network available - triggering sync")
                triggerImmediateSync()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d(TAG, "Network lost")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)

                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                if (hasInternet && isValidated) {
                    Log.d(TAG, "Network capabilities changed - validated internet available")
                    triggerImmediateSync()
                }
            }
        }

        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
            isNetworkCallbackRegistered = true
            Log.d(TAG, "Network callback registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    /**
     * Desregistrar callback de red
     */
    fun unregisterNetworkCallback() {
        if (!isNetworkCallbackRegistered || networkCallback == null) {
            return
        }

        try {
            connectivityManager.unregisterNetworkCallback(networkCallback!!)
            isNetworkCallbackRegistered = false
            Log.d(TAG, "Network callback unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister network callback", e)
        }
    }

    /**
     * Disparar sincronización inmediata
     */
    fun triggerImmediateSync() {
        Log.d(TAG, "Triggering immediate sync")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val immediateSyncRequest = OneTimeWorkRequestBuilder<DocumentSyncWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    DocumentSyncWorker.SYNC_MODE_KEY to DocumentSyncWorker.SYNC_MODE_BIDIRECTIONAL
                )
            )
            .addTag("immediate_sync")
            .build()

        workManager.enqueue(immediateSyncRequest)
    }

    /**
     * Disparar sincronización solo de subida (útil después de guardar)
     */
    fun triggerUploadSync(documentId: String? = null) {
        Log.d(TAG, "Triggering upload sync for document: ${documentId ?: "all"}")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadSyncRequest = OneTimeWorkRequestBuilder<DocumentSyncWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    DocumentSyncWorker.SYNC_MODE_KEY to DocumentSyncWorker.SYNC_MODE_UPLOAD,
                    DocumentSyncWorker.DOCUMENT_ID_KEY to documentId
                )
            )
            .addTag("upload_sync")
            .build()

        workManager.enqueue(uploadSyncRequest)
    }

    /**
     * Disparar sincronización solo de descarga
     */
    fun triggerDownloadSync() {
        Log.d(TAG, "Triggering download sync")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val downloadSyncRequest = OneTimeWorkRequestBuilder<DocumentSyncWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    DocumentSyncWorker.SYNC_MODE_KEY to DocumentSyncWorker.SYNC_MODE_DOWNLOAD
                )
            )
            .addTag("download_sync")
            .build()

        workManager.enqueue(downloadSyncRequest)
    }

    /**
     * Cancelar todas las sincronizaciones programadas
     */
    fun cancelAllSync() {
        Log.d(TAG, "Cancelling all sync work")
        workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
        workManager.cancelAllWorkByTag("sync")
    }

    /**
     * Verificar si hay red disponible
     */
    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Obtener información sobre trabajos de sincronización
     */
    fun getSyncWorkInfo() {
        workManager.getWorkInfosForUniqueWork(PERIODIC_SYNC_WORK_NAME)
            .get()
            .forEach { workInfo ->
                Log.d(TAG, "Sync work: ${workInfo.id}, state: ${workInfo.state}")
            }
    }
}
