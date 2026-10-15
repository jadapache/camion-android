package com.pronavicola.camion

import android.app.Application
import com.pronavicola.camion.service.MqttForegroundService
import com.pronavicola.camion.util.FileLogger
import com.pronavicola.camion.util.Log
import com.pronavicola.camion.worker.HistoricalDataCleanupWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CamionApplication : Application() {
    lateinit var appContext: CamionAppContext
        private set

    val isInitialized: Boolean
        get() = ::appContext.isInitialized

    override fun onCreate() {
        super.onCreate()

        // Initialize rolling file logger in app files directory
        try {
            val logDir = getExternalFilesDir(null) ?: filesDir
            FileLogger.instance = FileLogger(logDir)
            Log.i("CamionApplication", "Rolling file logger initialized at: ${logDir.absolutePath}")
        } catch (e: Exception) {
            Log.w("CamionApplication", "Failed to initialize rolling file logger: ${e.message}")
        }

        appContext = CamionAppContext(platformContext = this).apply {
            startBackgroundServices()
        }

        // Initialize persistent device identifier on first launch (Req 1.2, 1.3)
        applicationScope.launch {
            try {
                val deviceId = appContext.devicePairingService.initializeDeviceId()
                Log.i("CamionApplication", "Device ID initialized: $deviceId")
            } catch (e: Exception) {
                Log.e("CamionApplication", "Error initializing device ID: ${e.message}", e)
            }
        }

        // Schedule daily historical data cleanup at 03:00 AM (Req 11.1)
        try {
            HistoricalDataCleanupWorker.schedule(this)
            Log.i("CamionApplication", "Historical data cleanup worker scheduled")
        } catch (e: Exception) {
            Log.e("CamionApplication", "Error scheduling cleanup worker: ${e.message}", e)
        }

        // Start 24/7 foreground telemetry service
        try {
            MqttForegroundService.startService(this)
        } catch (e: Exception) {
            Log.e("CamionApplication", "Error starting MqttForegroundService: ${e.message}", e)
        }

        try {
            com.pronavicola.camion.service.HistoricalDataCleanupWorker.schedule(this)
        } catch (e: Exception) {
            Log.e("CamionApplication", "Error scheduling HistoricalDataCleanupWorker: ${e.message}")
        }
    }
}
