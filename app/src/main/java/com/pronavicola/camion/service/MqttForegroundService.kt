package com.pronavicola.camion.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.pronavicola.camion.CamionApplication
import com.pronavicola.camion.MainActivity
import com.pronavicola.camion.domain.model.MqttConnectionStatus
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Enterprise Android Foreground Service ensuring continuous 24/7 background telemetry:
 * 1. Holds a partial WakeLock during MQTT & telemetry operations.
 * 2. Runs GPS tracking and Modbus PLC data collection.
 * 3. Transmits telemetry & GPS to EMQX MQTT broker and synchronizes offline Outbox buffer.
 * 4. Adapts frequency dynamically according to network connectivity (WiFi vs Cellular).
 */
class MqttForegroundService : Service() {

    companion object {
        private const val TAG = "MqttForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "camion_telemetry_channel"
        private const val CHANNEL_NAME = "Telemetría de Camión Pronavícola"

        const val ACTION_START = "com.pronavicola.camion.service.START"
        const val ACTION_STOP = "com.pronavicola.camion.service.STOP"

        fun startService(context: Context) {
            try {
                val intent = Intent(context, MqttForegroundService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start MqttForegroundService: ${e.message}", e)
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, MqttForegroundService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop MqttForegroundService: ${e.message}", e)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private var statusCollectorJob: Job? = null
    private var networkMonitor: NetworkConnectivityMonitor? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        if (action == ACTION_STOP) {
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping foreground: ${e.message}")
            }
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val notification = createNotification("Iniciando servicio de telemetría y geolocalización...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var fgsType = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                if (hasLocationPermission()) {
                    fgsType = fgsType or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    fgsType,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to startForeground: ${e.message}", e)
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            startTelemetryServices()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting telemetry services: ${e.message}", e)
        }

        return START_REDELIVER_INTENT
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun startTelemetryServices() {
        val app = application as? CamionApplication ?: return
        if (!app.isInitialized) {
            Log.w(TAG, "CamionApplication appContext is not initialized yet")
            return
        }
        val appContainer = app.appContext

        // Ensure background services (Modbus polling coordinator + MQTT + GPS) are active
        appContainer.startBackgroundServices()

        // Dynamically update foreground notification based on connection state and GPS tracking
        statusCollectorJob?.cancel()
        statusCollectorJob = serviceScope.launch {
            combine(
                appContainer.mqttRepository.isConnected,
                appContainer.gpsLocationTracker.currentLocation
            ) { connected, location ->
                Pair(connected, location)
            }.collect { (connected, location) ->
                val modbusConnected = appContainer.modbusRepository.isConnected.value
                val pending = appContainer.mqttRepository.outboxPendingCount.value
                val statusText = buildString {
                    append(if (connected) "MQTT: OK" else "MQTT: Reconectando")
                    append(" | ")
                    append(if (modbusConnected) "PLC: OK" else "PLC: Desconectado")
                    append(" | ")
                    if (location != null) {
                        append("GPS: ${String.format(Locale.US, "%.4f, %.4f", location.latitud, location.longitud)}")
                    } else {
                        append("GPS: Rastreo activo")
                    }
                    if (pending > 0) append(" | Pendientes: $pending")
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Monitoreo continuo de temperatura, humedad, geolocalización y alarmas de transporte"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitoreo de Furgón Pronavícola")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(contentText: String) {
        try {
            val notification = createNotification(contentText)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Error updating notification: ${e.message}")
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "$TAG::ContinuousTelemetryWakeLock",
            ).apply {
                setReferenceCounted(false)
                acquire(24 * 60 * 60 * 1000L) // 24 hours max per acquire
            }
            Log.i(TAG, "WakeLock acquired: background telemetry will run with screen OFF")
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire WakeLock: ${e.message}")
        }
    }

    override fun onDestroy() {
        networkMonitor?.stopMonitoring()
        networkMonitor = null
        statusCollectorJob?.cancel()
        serviceScope.cancel()

        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing WakeLock: ${e.message}")
        }

        Log.i(TAG, "MqttForegroundService destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
