package com.pronavicola.camion.data.mqtt

import com.pronavicola.camion.data.local.dao.TrazasUbicacionDao
import com.pronavicola.camion.data.local.dao.VariableRegistroDao
import com.pronavicola.camion.data.modbus.ModbusClient
import com.pronavicola.camion.domain.model.GpsLocationPayload
import com.pronavicola.camion.domain.model.MqttConnectionStatus
import com.pronavicola.camion.domain.model.TelemetryPayload
import com.pronavicola.camion.domain.repository.AppConfigRepository
import com.pronavicola.camion.util.CircularBuffer
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class EnhancedTelemetryMqttCoordinator(
    private val modbusClient: ModbusClient,
    private val mqttClientManager: MqttClientManager,
    private val outboxManager: OutboxQueueManager,
    private val trazasUbicacionDao: TrazasUbicacionDao,
    private val variableRegistroDao: VariableRegistroDao,
    private val appConfigRepository: AppConfigRepository,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val tag = "EnhancedTelemetryMqttCoordinator"

    private var telemetryJob: Job? = null
    private var gpsJob: Job? = null
    private var heartbeatJob: Job? = null
    private var connectionMonitorJob: Job? = null

    val publishedMessageIds = CircularBuffer<String>(capacity = 1000)
    var lastGpsPublishTime: Long = 0L

    private val _connectionStatus = MutableStateFlow(MqttConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<MqttConnectionStatus> = _connectionStatus.asStateFlow()

    private val _lastTelemetryPublish = MutableStateFlow<Long?>(null)
    val lastTelemetryPublish: StateFlow<Long?> = _lastTelemetryPublish.asStateFlow()

    private val _lastGpsPublish = MutableStateFlow<Long?>(null)
    val lastGpsPublish: StateFlow<Long?> = _lastGpsPublish.asStateFlow()

    companion object {
        const val TELEMETRY_INTERVAL_SEC = 30L
        const val GPS_INTERVAL_SEC = 120L
        const val HEARTBEAT_INTERVAL_SEC = 30L
        const val GPS_DEBOUNCE_SEC = 60L
        const val MAX_CONSECUTIVE_FAILURES = 3
    }

    var consecutiveFailures = 0

    fun start() {
        stop()

        connectionMonitorJob = scope.launch(ioDispatcher) {
            mqttClientManager.isConnected.collect { connected ->
                if (connected) {
                    _connectionStatus.value = MqttConnectionStatus.CONNECTED
                    consecutiveFailures = 0
                } else {
                    if (_connectionStatus.value != MqttConnectionStatus.CONNECTING) {
                        _connectionStatus.value = MqttConnectionStatus.DISCONNECTED
                    }
                }
            }
        }

        // Telemetry loop (30s)
        telemetryJob = scope.launch(ioDispatcher) {
            while (isActive) {
                val intervalSec = appConfigRepository.getPublishInterval()?.toLongOrNull() ?: TELEMETRY_INTERVAL_SEC
                val deviceId = getDeviceId()
                publishTelemetry(deviceId)
                delay(intervalSec * 1000L)
            }
        }

        // GPS loop (120s)
        gpsJob = scope.launch(ioDispatcher) {
            while (isActive) {
                val deviceId = getDeviceId()
                publishGpsLocation(deviceId)
                delay(GPS_INTERVAL_SEC * 1000L)
            }
        }

        // Heartbeat loop (30s)
        heartbeatJob = scope.launch(ioDispatcher) {
            while (isActive) {
                val deviceId = getDeviceId()
                publishHeartbeat(deviceId)
                delay(HEARTBEAT_INTERVAL_SEC * 1000L)
            }
        }

        Log.i(tag, "Enhanced telemetry coordinator started")
    }

    fun stop() {
        telemetryJob?.cancel()
        telemetryJob = null
        gpsJob?.cancel()
        gpsJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        connectionMonitorJob?.cancel()
        connectionMonitorJob = null
        Log.i(tag, "Enhanced telemetry coordinator stopped")
    }

    private suspend fun getDeviceId(): String {
        return appConfigRepository.getDeviceId() ?: "AAA000"
    }

    suspend fun publishTelemetry(deviceId: String) {
        try {
            val isModbusConnected = modbusClient.isConnected.value
            val t1 = modbusClient.temperatura1.value
            val t2 = modbusClient.temperatura2.value
            val hum = modbusClient.humedad.value
            val vlt = modbusClient.tension.value
            val proceso = modbusClient.procesoActivo.value
            val zonas = modbusClient.zonas.value
            val irrigator = modbusClient.estadoIrrigador.value
            val damper = modbusClient.damperActivo.value
            val calefaccion = modbusClient.calefaccionActiva.value
            val alarmas = modbusClient.alarmasActivas.value

            if (!isModbusConnected) {
                publishModbusDisconnectedStatus(deviceId)
                return
            }

            val messageId = UUID.randomUUID().toString()
            if (publishedMessageIds.contains(messageId)) {
                Log.w(tag, "Skipping duplicate telemetry message_id: $messageId")
                return
            }

            val payload = TelemetryPayload(
                device_id = deviceId,
                message_id = messageId,
                timestamp = System.currentTimeMillis(),
                temperature_c = t1,
                temperature_2_c = t2,
                humidity_pct = hum,
                voltage_v = vlt,
                proceso_activo = proceso,
                zonas = zonas.map { it.name },
                estado_irrigador = irrigator.name,
                damper_activo = damper,
                calefaccion_activa = calefaccion,
                alarmas = alarmas,
            )

            val topic = "furgones/$deviceId/telemetry"
            val success = publishMessage(topic, payload.toJson(), 1, messageId)

            if (success) {
                publishedMessageIds.add(messageId)
                _lastTelemetryPublish.value = System.currentTimeMillis()
                consecutiveFailures = 0
                _connectionStatus.value = MqttConnectionStatus.CONNECTED

                // Update pending registros to ENVIADO
                val pending = variableRegistroDao.getPendingRegistros(1)
                pending.firstOrNull()?.let {
                    variableRegistroDao.updateEstadoEnvio(it.idRegistro, "ENVIADO")
                }
                Log.d(tag, "Telemetry published successfully to $topic [payload size: ${payload.toJson().length}]")
            } else {
                consecutiveFailures++
                val pending = variableRegistroDao.getPendingRegistros(1)
                pending.firstOrNull()?.let {
                    variableRegistroDao.updateEstadoEnvio(it.idRegistro, "PENDIENTE")
                }
                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    _connectionStatus.value = MqttConnectionStatus.ERROR
                    Log.w(tag, "Max consecutive failures reached ($consecutiveFailures), resetting connection")
                    resetConnection()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error publishing telemetry: ${e.message}", e)
            consecutiveFailures++
        }
    }

    suspend fun publishGpsLocation(deviceId: String) {
        try {
            val currentTime = System.currentTimeMillis()

            // Debounce check (minimum 60s window)
            if (currentTime - lastGpsPublishTime < GPS_DEBOUNCE_SEC * 1000L) {
                Log.d(tag, "GPS publish skipped by debounce filter (< ${GPS_DEBOUNCE_SEC}s since last)")
                return
            }

            val latestLocation = trazasUbicacionDao.getLatestTrace()
            if (latestLocation == null) {
                Log.d(tag, "No GPS location trace found to publish")
                return
            }

            val messageId = UUID.randomUUID().toString()
            if (publishedMessageIds.contains(messageId)) {
                Log.w(tag, "Skipping duplicate GPS message_id: $messageId")
                return
            }

            val payload = GpsLocationPayload(
                device_id = deviceId,
                message_id = messageId,
                timestamp = latestLocation.fechaHora,
                latitude = latestLocation.latitud,
                longitude = latestLocation.longitud,
                altitude = latestLocation.altitud,
                accuracy = latestLocation.precision,
                speed = latestLocation.velocidad,
            )

            val topic = "furgones/$deviceId/location"
            val success = publishMessage(topic, payload.toJson(), 1, messageId)

            if (success) {
                publishedMessageIds.add(messageId)
                lastGpsPublishTime = currentTime
                _lastGpsPublish.value = currentTime
                Log.i(tag, "GPS location published to $topic: lat=${latestLocation.latitud}, lon=${latestLocation.longitud}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error publishing GPS location: ${e.message}", e)
        }
    }

    suspend fun publishHeartbeat(deviceId: String) {
        try {
            val status = if (modbusClient.isConnected.value) "ONLINE" else "MODBUS_DISCONNECTED"
            val topic = "furgones/$deviceId/status"
            val payload = """{"device_id":"$deviceId","status":"$status","timestamp":${System.currentTimeMillis()}}"""
            publishMessage(topic, payload, 1, UUID.randomUUID().toString())
        } catch (e: Exception) {
            Log.w(tag, "Error publishing heartbeat: ${e.message}")
        }
    }

    private suspend fun publishModbusDisconnectedStatus(deviceId: String) {
        val topic = "furgones/$deviceId/status"
        val payload = """{"device_id":"$deviceId","status":"MODBUS_DISCONNECTED","timestamp":${System.currentTimeMillis()}}"""
        publishMessage(topic, payload, 1, UUID.randomUUID().toString())
    }

    private suspend fun publishMessage(topic: String, payload: String, qos: Int, messageId: String): Boolean {
        return try {
            val result = mqttClientManager.publish(topic, payload, qos)
            if (result.isSuccess) {
                true
            } else {
                outboxManager.enqueue(topic, payload, qos, messageId)
                Log.w(tag, "MQTT publish failed, enqueued to outbox: $topic")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "MQTT publish exception, enqueued to outbox: ${e.message}")
            outboxManager.enqueue(topic, payload, qos, messageId)
            false
        }
    }

    private suspend fun resetConnection() {
        Log.i(tag, "Resetting MQTT connection...")
        consecutiveFailures = 0
        _connectionStatus.value = MqttConnectionStatus.CONNECTING
        mqttClientManager.stop()
        delay(2000L)
        val broker = appConfigRepository.getBrokerUrl() ?: "tcp://mqtt.pronavicola.com:1883"
        val devId = appConfigRepository.getDeviceId() ?: "AAA000"
        val user = appConfigRepository.getBrokerUsername()
        val pass = appConfigRepository.getBrokerPassword()
        mqttClientManager.start(broker, devId, user, pass)
    }
}
