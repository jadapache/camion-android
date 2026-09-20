package com.pronavicola.camion.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.pronavicola.camion.CamionApplication
import com.pronavicola.camion.MainActivity
import com.pronavicola.camion.R
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Enterprise Android Foreground Service ensuring continuous 24/7 background telemetry:
 * 1. Holds a partial WakeLock to prevent CPU sleep when screen is OFF.
 * 2. Continuously samples Modbus PLC data and records VariableRegistroEntity in SQLite.
 * 3. Transmits telemetry to EMQX MQTT broker and synchronizes offline Outbox buffer.
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
            val intent = Intent(context, MqttForegroundService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MqttForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private var statusCollectorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification("Iniciando servicio de telemetría y registro...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startTelemetryServices()

        return START_STICKY
    }

    private fun startTelemetryServices() {
        val app = application as? CamionApplication ?: return
        val appContainer = app.appContext

        // Ensure background services (Modbus polling coordinator + MQTT) are active
        appContainer.startBackgroundServices()

        // Dynamically update foreground notification based on connection state
        statusCollectorJob?.cancel()
        statusCollectorJob = serviceScope.launch {
            appContainer.mqttRepository.isConnected.collect { connected ->
                val modbusConnected = appContainer.modbusRepository.isConnected.value
                val pending = appContainer.mqttRepository.outboxPendingCount.value
                val statusText = buildString {
                    append(if (connected) "MQTT: Conectado" else "MQTT: Reconectando")
                    append(" | ")
                    append(if (modbusConnected) "PLC: OK" else "PLC: Desconectado")
                    if (pending > 0) append(" | Pendientes: $pending")
                }
                updateNotification(statusText)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Monitoreo continuo de temperatura, humedad y alarmas de transporte"
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
        val notification = createNotification(contentText)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
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
        super.onDestroy()
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
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
