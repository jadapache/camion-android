package com.pronavicola.camion.data.mqtt

import com.pronavicola.camion.util.Log
import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.data.modbus.ModbusClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Coordinates telemetry data gathering and routes payloads
 * through the OutboxQueueManager to EMQX MQTT topics using standard schema:
 * - furgones/{device_id}/telemetry
 * - furgones/{device_id}/location
 * - furgones/{device_id}/alarms
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
    private var samplingIntervalMs: Long = TELEMETRY_INTERVAL_MS
    private var coordinatorJob: Job? = null
    private var heartbeatJob: Job? = null

    companion object {
        const val TELEMETRY_INTERVAL_MS = 30000L
        const val HEARTBEAT_INTERVAL_MS = 30000L

        // Thresholds for local alarm evaluation
        const val TEMP_HIGH_THRESHOLD = 30.0
        const val HUMIDITY_HIGH_THRESHOLD = 90.0
        const val VOLTAGE_LOW_THRESHOLD = 18.0
    }

    fun start(deviceId: String = activeDeviceId) {
        activeDeviceId = deviceId
        stop()
        Log.i(tag, "TelemetryMqttCoordinator started for $activeDeviceId")
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

    fun updateTopicRoot(root: String) {
        if (root.isNotBlank()) {
            MqttTopics.topicRoot = root.trim()
        }
    }

    fun setPublishIntervalSeconds(seconds: Long) {
        samplingIntervalMs = (seconds * 1000L).coerceAtLeast(1000L)
    }

    /**
     * Enqueues a single VariableRegistroEntity record to the MQTT outbox queue.
     * Contains solely the physical telemetry variables: T1, T2, Humedad, Tension.
     */
    fun enqueueVariableRegistro(registro: VariableRegistroEntity) {
        val timestamp = parseDateToEpoch(registro.fecha)
        val payload = TelemetryMqttPayload(
            deviceId = activeDeviceId,
            timestamp = timestamp,
            temperatureC = registro.temperatura1,
            temperature2C = registro.temperatura2,
            humidityPct = registro.humedad,
            voltageV = registro.tension,
        )
        val topic = MqttTopics.telemetry(activeDeviceId)
        Log.d(tag, "Enqueuing VariableRegistro for $activeDeviceId to $topic (Fecha: ${registro.fecha})")
        scope.launch(ioDispatcher) {
            outboxManager.enqueue(topic, payload.toJson(), qos = 1)
            evaluateLocalAlarms(registro.temperatura1, registro.temperatura2, registro.humedad, registro.tension)
        }
    }

    /**
     * Enqueues a location payload to the MQTT outbox queue: furgones/{device_id}/location
     */
    fun enqueueLocation(latitude: Double, longitude: Double, speedKmh: Double? = null, altitudeM: Double? = null) {
        val payload = LocationMqttPayload(
            deviceId = activeDeviceId,
            latitude = latitude,
            longitude = longitude,
            speedKmh = speedKmh,
            altitudeM = altitudeM,
        )
        val topic = MqttTopics.location(activeDeviceId)
        Log.d(tag, "Enqueuing Location for $activeDeviceId to $topic")
        scope.launch(ioDispatcher) {
            outboxManager.enqueue(topic, payload.toJson(), qos = 1)
        }
    }

    /**
     * Enqueues a list of VariableRegistroEntity records to the MQTT outbox and triggers an immediate drain.
     */
    fun syncAllVariableRegistros(registros: List<VariableRegistroEntity>) {
        registros.forEach { enqueueVariableRegistro(it) }
        outboxManager.triggerDrain()
    }

    private fun parseDateToEpoch(dateStr: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    private suspend fun evaluateLocalAlarms(t1: Double?, t2: Double?, hum: Double?, vlt: Double?) {
        val maxTemp = maxOf(t1 ?: -100.0, t2 ?: -100.0)
        val alarmTopic = MqttTopics.alarms(activeDeviceId)
        if (maxTemp > TEMP_HIGH_THRESHOLD) {
            val alarm = AlarmMqttPayload(
                deviceId = activeDeviceId,
                alarmType = "TEMP_HIGH",
                severity = "WARNING",
                triggerValue = maxTemp,
            )
            outboxManager.enqueue(alarmTopic, alarm.toJson(), qos = 1)
        }

        if ((hum ?: 0.0) > HUMIDITY_HIGH_THRESHOLD) {
            val alarm = AlarmMqttPayload(
                deviceId = activeDeviceId,
                alarmType = "HUMIDITY_HIGH",
                severity = "WARNING",
                triggerValue = hum ?: 0.0,
            )
            outboxManager.enqueue(alarmTopic, alarm.toJson(), qos = 1)
        }

        if (vlt != null && vlt > 0.0 && vlt < VOLTAGE_LOW_THRESHOLD) {
            val alarm = AlarmMqttPayload(
                deviceId = activeDeviceId,
                alarmType = "VOLTAGE_LOW",
                severity = "CRITICAL",
                triggerValue = vlt,
            )
            outboxManager.enqueue(alarmTopic, alarm.toJson(), qos = 1)
        }
    }
}
