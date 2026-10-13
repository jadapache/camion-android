package com.pronavicola.camion

import android.app.Application
import com.pronavicola.camion.service.MqttForegroundService
import com.pronavicola.camion.util.Log

class CamionApplication : Application() {
    lateinit var appContext: CamionAppContext
        private set

    override fun onCreate() {
        super.onCreate()
        appContext = CamionAppContext(platformContext = this).apply {
            startBackgroundServices()
        }

        try {
            MqttForegroundService.startService(this)
        } catch (e: Exception) {
            Log.e("CamionApplication", "Error starting MqttForegroundService: ${e.message}")
        }

        try {
            com.pronavicola.camion.service.HistoricalDataCleanupWorker.schedule(this)
        } catch (e: Exception) {
            Log.e("CamionApplication", "Error scheduling HistoricalDataCleanupWorker: ${e.message}")
        }
    }
}
