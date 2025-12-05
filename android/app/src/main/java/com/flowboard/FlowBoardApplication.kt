package com.flowboard

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.flowboard.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FlowBoardApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()

        // Inicializar el gestor de sincronización offline-first
        // Esto programa sincronizaciones periódicas y detecta cambios de red
        syncManager.initialize()
    }

    override fun onTerminate() {
        super.onTerminate()
        // Limpiar recursos cuando la app se termina
        syncManager.unregisterNetworkCallback()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
