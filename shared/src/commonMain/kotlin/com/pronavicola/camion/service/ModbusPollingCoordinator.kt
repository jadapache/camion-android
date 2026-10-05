package com.pronavicola.camion.service

import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.data.modbus.ModbusClient
import com.pronavicola.camion.data.mqtt.MqttClientManager
import com.pronavicola.camion.data.mqtt.TelemetryMqttCoordinator
import com.pronavicola.camion.domain.repository.AppConfigRepository
import com.pronavicola.camion.domain.repository.RegistrosRepository
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Multiplatform continuous coordinator for polling Modbus PLC,
 * persisting telemetry records directly to SQLite (VariableRegistroEntity),
 * and forwarding them to the MQTT telemetry publisher.
 *
 * Runs seamlessly in background services, keeping data recording active
 * even when the screen is turned off or the app is minimized.
 */
class ModbusPollingCoordinator(
    private val modbusClient: ModbusClient,
    private val registrosRepository: RegistrosRepository,
    private val appConfigRepository: AppConfigRepository,
    private val telemetryMqttCoordinator: TelemetryMqttCoordinator,
    private val mqttClientManager: MqttClientManager,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val tag = "ModbusPollingCoordinator"
    private var pollingJob: Job? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        pollingJob?.cancel()

        pollingJob = scope.launch(ioDispatcher) {
            Log.i(tag, "Starting continuous Modbus polling and SQLite persistence loop...")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            while (isActive) {
                try {
                    val isConnected = modbusClient.isConnected.value
                    val t1 = modbusClient.temperatura1.value
                    val t2 = modbusClient.temperatura2.value
                    val hum = modbusClient.humedad.value
                    val tension = modbusClient.tension.value
                    val procesoActivo = modbusClient.procesoActivo.value

                    // Condition: When Modbus is disconnected, do not save dummy zeros or stale telemetry
                    if (isConnected && (t1 != null || t2 != null || hum != null)) {
                        val deviceId = appConfigRepository.getDeviceId() ?: "ABC123"
                        val isMqttConnected = mqttClientManager.isConnected.value

                        val registro = VariableRegistroEntity(
                            idVehiculo = 1,
                            fecha = dateFormat.format(Date()),
                            temperatura1 = t1 ?: 0.0,
                            temperatura2 = t2 ?: 0.0,
                            humedad = hum ?: 0.0,
                            tension = tension ?: 0.0,
                            estadoEnvio = if (isMqttConnected) "Enviado" else "Pendiente",
                        )

                        // 1. Persist to local SQLite database
                        registrosRepository.saveRegistro(registro)
                        Log.d(tag, "Saved telemetry record to SQLite: T1=$t1, T2=$t2, Hum=$hum, Tension=$tension, Estado=${registro.estadoEnvio}")

                        // 2. Transmit via MQTT outbox coordinator
                        telemetryMqttCoordinator.enqueueVariableRegistro(registro)
                    } else if (!isConnected) {
                        Log.v(tag, "Modbus disconnected: skipping SQLite persistence to avoid recording zero/stale values.")
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Error during Modbus polling/persistence tick: ${e.message}")
                }

                // DB persistence & telemetry enqueue interval from app config or default 30 seconds
                val intervalSeconds = appConfigRepository.getPublishInterval()?.toLongOrNull() ?: 30L
                val delayMs = (intervalSeconds * 1000L).coerceIn(2000L, 60000L)
                delay(delayMs)
            }
        }
    }

    fun stop() {
        isRunning = false
        pollingJob?.cancel()
        pollingJob = null
        Log.i(tag, "Modbus polling and persistence loop stopped.")
    }
}
