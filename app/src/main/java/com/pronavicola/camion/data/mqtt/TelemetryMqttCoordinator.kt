package com.pronavicola.camion.data.mqtt

import android.util.Log
import com.pronavicola.camion.data.modbus.ModbusClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Coordinates telemetry data gathering from ModbusClient and routes payloads
 * through the OutboxQueueManager to EMQX MQTT topics.
 */
class TelemetryMqttCoordinator(
    private val modbusClient: ModbusClient,
    private val outboxManager: OutboxQueueManager,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDeviceId: String = "AAA000",
) {
    private val tag = "TelemetryMqttCoordinator"
    private var activeDeviceId: String = defaultDeviceId
    private var coordinatorJob: Job? = null
    private var heartbeatJob: Job? = null

    companion object {
        const val TELEMETRY_INTERVAL_MS = 5000L
        const val HEARTBEAT_INTERVAL_MS = 30000L

        // Thresholds for local alarm evaluation
        const val TEMP_HIGH_THRESHOLD = 30.0
        const val HUMIDITY_HIGH_THRESHOLD = 90.0
        const val VOLTAGE_LOW_THRESHOLD = 18.0
    }

    fun start(deviceId: String = activeDeviceId) {
        activeDeviceId = deviceId
        stop()

        coordinatorJob = scope.launch(ioDispatcher) {
            startTelemetryPublishingLoop()
        }

        heartbeatJob = scope.launch(ioDispatcher) {
            startHeartbeatLoop()
        }
    }

    fun stop() {
        coordinatorJob?.cancel()
        coordinatorJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun updateDeviceId(deviceId: String) {
        activeDeviceId = deviceId
    }

    private suspend fun startTelemetryPublishingLoop() {
        while (scope.isActive) {
            try {
                val t1 = modbusClient.temperatura1.value
                val t2 = modbusClient.temperatura2.value
                val hum = modbusClient.humedad.value
                val vlt = modbusClient.tension.value
                val proceso = modbusClient.procesoActivo.value
                val zonas = modbusClient.zonas.value
                val irrigator = modbusClient.estadoIrrigador.value

                // Publish telemetry if connected or has readings
                if (t1 != null || t2 != null || hum != null) {
                    val payload = TelemetryMqttPayload(
                        deviceId = activeDeviceId,
                        temperatureC = t1,
                        temperature2C = t2,
                        humidityPct = hum,
                        voltageV = vlt,
                        procesoActivo = proceso,
                        zonas = zonas,
                        estadoIrrigador = irrigator,
                    )
                    val topic = "furgones/$activeDeviceId/telemetry"
                    outboxManager.enqueue(topic, payload.toJson(), qos = 1)

                    // Local alarm evaluation
                    evaluateLocalAlarms(t1, t2, hum, vlt)
                }
            } catch (e: Exception) {
                Log.w(tag, "Error during telemetry coordination tick: ${e.message}")
            }

            delay(TELEMETRY_INTERVAL_MS)
        }
    }

    private suspend fun startHeartbeatLoop() {
        while (scope.isActive) {
            try {
                val status = if (modbusClient.isConnected.value) "ONLINE" else "MODBUS_DISCONNECTED"
                val payload = StatusMqttPayload(
                    deviceId = activeDeviceId,
                    status = status,
                )
                val topic = "furgones/$activeDeviceId/status"
                outboxManager.enqueue(topic, payload.toJson(), qos = 1)
            } catch (e: Exception) {
                Log.w(tag, "Error during heartbeat tick: ${e.message}")
            }

            delay(HEARTBEAT_INTERVAL_MS)
        }
    }

    private fun evaluateLocalAlarms(t1: Double?, t2: Double?, hum: Double?, vlt: Double?) {
        val maxTemp = maxOf(t1 ?: -100.0, t2 ?: -100.0)
        if (maxTemp > TEMP_HIGH_THRESHOLD) {
            val alarm = AlarmMqttPayload(
                deviceId = activeDeviceId,
                alarmType = "TEMP_HIGH",
                severity = "WARNING",
                triggerValue = maxTemp,
            )
            outboxManager.enqueue("furgones/$activeDeviceId/alarms", alarm.toJson(), qos = 1)
        }

        if ((hum ?: 0.0) > HUMIDITY_HIGH_THRESHOLD) {
            val alarm = AlarmMqttPayload(
                deviceId = activeDeviceId,
                alarmType = "HUMIDITY_HIGH",
                severity = "WARNING",
                triggerValue = hum ?: 0.0,
            )
            outboxManager.enqueue("furgones/$activeDeviceId/alarms", alarm.toJson(), qos = 1)
        }

        if (vlt != null && vlt > 0.0 && vlt < VOLTAGE_LOW_THRESHOLD) {
            val alarm = AlarmMqttPayload(
                deviceId = activeDeviceId,
                alarmType = "VOLTAGE_LOW",
                severity = "CRITICAL",
                triggerValue = vlt,
            )
            outboxManager.enqueue("furgones/$activeDeviceId/alarms", alarm.toJson(), qos = 1)
        }
    }
}
