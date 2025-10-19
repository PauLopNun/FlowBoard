package com.flowboard

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FlowBoardApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any app-wide components here
    }
}
