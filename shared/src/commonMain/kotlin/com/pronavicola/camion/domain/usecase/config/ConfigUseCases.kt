package com.pronavicola.camion.domain.usecase.config

import com.pronavicola.camion.domain.repository.AppConfigRepository
import com.pronavicola.camion.domain.repository.DevicePairingRepository
import com.pronavicola.camion.domain.repository.MqttRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppConfigData(
    val deviceId: String,
    val furgonPlate: String,
    val brokerUrl: String,
    val topicRoot: String,
    val brokerUsername: String,
    val brokerPassword: String,
    val modbusHost: String,
    val modbusPort: String,
    val publishInterval: String,
    val serverBaseUrl: String,
    val deviceToken: String?,
    val pairingCode: String?,
    val licenseKey: String?,
    val licenseStatus: String?,
    val modbusSimulationEnabled: Boolean,
)

class GetAppConfigUseCase(
    private val appConfigRepository: AppConfigRepository,
) {
    suspend operator fun invoke(): AppConfigData = withContext(Dispatchers.Default) {
        AppConfigData(
            deviceId = appConfigRepository.getDeviceId() ?: "ABC123",
            furgonPlate = appConfigRepository.getFurgonPlate() ?: "AAA000",
            brokerUrl = appConfigRepository.getBrokerUrl() ?: "",
            topicRoot = appConfigRepository.getTopicRoot() ?: "furgones",
            brokerUsername = appConfigRepository.getBrokerUsername() ?: "",
            brokerPassword = appConfigRepository.getBrokerPassword() ?: "",
            modbusHost = appConfigRepository.getModbusHost() ?: "",
            modbusPort = appConfigRepository.getModbusPort() ?: "",
            publishInterval = appConfigRepository.getPublishInterval() ?: "30",
            serverBaseUrl = appConfigRepository.getServerBaseUrl() ?: "",
            deviceToken = appConfigRepository.getDeviceToken(),
            pairingCode = appConfigRepository.getPairingCode(),
            licenseKey = appConfigRepository.getLicenseKey(),
            licenseStatus = appConfigRepository.getLicenseStatus(),
            modbusSimulationEnabled = appConfigRepository.getModbusSimulationEnabled(),
        )
    }
}

class SaveAppConfigUseCase(
    private val appConfigRepository: AppConfigRepository,
) {
    suspend operator fun invoke(
        brokerUrl: String? = null,
        brokerUsername: String? = null,
        brokerPassword: String? = null,
        modbusHost: String? = null,
        modbusPort: String? = null,
        publishInterval: String? = null,
        deviceId: String? = null,
        furgonPlate: String? = null,
    ) = withContext(Dispatchers.Default) {
        brokerUrl?.let { appConfigRepository.saveBrokerUrl(it) }
        brokerUsername?.let { appConfigRepository.saveBrokerUsername(it) }
        brokerPassword?.let { appConfigRepository.saveBrokerPassword(it) }
        modbusHost?.let { appConfigRepository.saveModbusHost(it) }
        modbusPort?.let { appConfigRepository.saveModbusPort(it) }
        publishInterval?.let { appConfigRepository.savePublishInterval(it) }
        deviceId?.let { appConfigRepository.saveDeviceId(it) }
        furgonPlate?.let { appConfigRepository.saveFurgonPlate(it) }
    }
}

class RestartMqttClientUseCase(
    private val appConfigRepository: AppConfigRepository,
    private val mqttRepository: MqttRepository,
) {
    suspend operator fun invoke() = withContext(Dispatchers.Default) {
        val brokerUrl = appConfigRepository.getBrokerUrl() ?: "tcp://181.129.185.230:1883"
        val deviceId = appConfigRepository.getDeviceId() ?: "ABC123"
        val username = appConfigRepository.getBrokerUsername() ?: "METALTECO"
        val password = appConfigRepository.getBrokerPassword() ?: "autometalteco"

        mqttRepository.start(brokerUrl, "camion_$deviceId", username, password)
    }
}

data class TestMqttOutput(
    val message: String,
    val payloadJson: String,
)

class TestMqttConnectionUseCase(
    private val appConfigRepository: AppConfigRepository,
) {
    suspend operator fun invoke(
        brokerUrl: String? = null,
        username: String? = null,
        password: String? = null,
        deviceId: String? = null,
    ): Result<TestMqttOutput> = withContext(Dispatchers.IO) {
        val targetBrokerUrl = brokerUrl?.takeIf { it.isNotBlank() }
            ?: appConfigRepository.getBrokerUrl()?.takeIf { it.isNotBlank() }

        if (targetBrokerUrl.isNullOrBlank()) {
            return@withContext Result.failure(Exception("Debe ingresar la URL del broker MQTT para realizar la prueba."))
        }

        val targetDeviceId = deviceId?.takeIf { it.isNotBlank() }
            ?: appConfigRepository.getDeviceId()
            ?: "ABC123"
        val targetUser = username ?: appConfigRepository.getBrokerUsername() ?: ""
        val targetPass = password ?: appConfigRepository.getBrokerPassword() ?: ""

        val sanitizedUrl = when {
            targetBrokerUrl.startsWith("mqtt://") -> "tcp://" + targetBrokerUrl.removePrefix("mqtt://")
            !targetBrokerUrl.startsWith("tcp://") && !targetBrokerUrl.startsWith("ssl://") -> "tcp://$targetBrokerUrl"
            else -> targetBrokerUrl
        }

        val testClientId = "camion_test_${(System.currentTimeMillis() % 100000)}"
        val client = org.eclipse.paho.client.mqttv3.MqttAsyncClient(
            sanitizedUrl,
            testClientId,
            org.eclipse.paho.client.mqttv3.persist.MemoryPersistence()
        )

        val options = org.eclipse.paho.client.mqttv3.MqttConnectOptions().apply {
            isCleanSession = true
            connectionTimeout = 5
            keepAliveInterval = 30
            if (targetUser.isNotBlank()) {
                userName = targetUser
            }
            if (targetPass.isNotBlank()) {
                this.password = targetPass.toCharArray()
            }
        }

        val connectSuccess = kotlinx.coroutines.suspendCancellableCoroutine<Boolean> { cont ->
            try {
                client.connect(options, null, object : org.eclipse.paho.client.mqttv3.IMqttActionListener {
                    override fun onSuccess(asyncActionToken: org.eclipse.paho.client.mqttv3.IMqttToken?) {
                        cont.resumeWith(Result.success(true))
                    }

                    override fun onFailure(asyncActionToken: org.eclipse.paho.client.mqttv3.IMqttToken?, exception: Throwable?) {
                        cont.resumeWith(Result.success(false))
                    }
                })
            } catch (e: Exception) {
                cont.resumeWith(Result.success(false))
            }
        }

        if (!connectSuccess || !client.isConnected) {
            try {
                client.close()
            } catch (_: Exception) {}
            return@withContext Result.failure(Exception("No se pudo conectar al broker MQTT en $sanitizedUrl"))
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
        val telemetryTopic = "furgones/$targetDeviceId/telemetry"
        val locationTopic = "furgones/$targetDeviceId/location"
        val alarmTopic = "furgones/$targetDeviceId/alarms"

        val telemetryPayload = """{
  "device_id": "$targetDeviceId",
  "timestamp": "$timestamp",
  "test": true,
  "temperature_c": 21.5,
  "temperature_2_c": 22.0,
  "humidity_pct": 65.0,
  "voltage_v": 24.0
}"""
        val locationPayload = """{"device_id":"$targetDeviceId","timestamp":"$timestamp","test":true,"latitude":4.7110,"longitude":-74.0721,"speed":0.0}"""
        val alarmPayload = """{"device_id":"$targetDeviceId","timestamp":"$timestamp","test":true,"alarms":[],"status":"NORMAL"}"""

        fun publishMessage(topic: String, payload: String): Boolean {
            return try {
                val msg = org.eclipse.paho.client.mqttv3.MqttMessage(payload.toByteArray(Charsets.UTF_8)).apply { qos = 1 }
                val token = client.publish(topic, msg)
                token.waitForCompletion(3000)
                true
            } catch (_: Exception) {
                false
            }
        }

        val tSent = publishMessage(telemetryTopic, telemetryPayload)
        val lSent = publishMessage(locationTopic, locationPayload)
        val aSent = publishMessage(alarmTopic, alarmPayload)

        try {
            client.disconnect(1000)
            client.close()
        } catch (_: Exception) {}

        if (tSent && lSent && aSent) {
            Result.success(
                TestMqttOutput(
                    message = "¡Conexión exitosa! Paquetes de prueba transmitidos correctamente a los tópicos telemetry, location y alarms.",
                    payloadJson = telemetryPayload,
                )
            )
        } else if (tSent || lSent || aSent) {
            Result.success(
                TestMqttOutput(
                    message = "Conectado. Enviados parcialmente: telemetry=$tSent, location=$lSent, alarms=$aSent",
                    payloadJson = telemetryPayload,
                )
            )
        } else {
            Result.failure(Exception("Conectó al broker pero falló la publicación en los tópicos."))
        }
    }
}

class PairDeviceUseCase(
    private val devicePairingRepository: DevicePairingRepository,
) {
    fun getHardwareFingerprint(): String = devicePairingRepository.getHardwareFingerprint()

    suspend fun requestPairingCode(serverUrl: String): Result<Pair<String, String>> =
        devicePairingRepository.requestPairingCode(serverUrl)

    suspend fun checkPairingStatus(serverUrl: String, pairingCode: String, deviceId: String): Result<Boolean> =
        devicePairingRepository.checkPairingStatus(serverUrl, pairingCode, deviceId)

    suspend fun activateLicenseKey(serverUrl: String, licenseKey: String): Result<Boolean> =
        devicePairingRepository.activateLicenseKey(serverUrl, licenseKey)

    suspend fun unpairDevice(serverUrl: String, deviceToken: String?): Result<Unit> =
        devicePairingRepository.unpairDevice(serverUrl, deviceToken)

    suspend fun getAvailableFurgones(serverUrl: String, deviceToken: String?): Result<List<com.pronavicola.camion.data.remote.FurgonInfo>> =
        devicePairingRepository.getAvailableFurgones(serverUrl, deviceToken)

    suspend fun updateAssociatedFurgon(serverUrl: String, deviceToken: String?, furgonId: String): Result<String> =
        devicePairingRepository.updateAssociatedFurgon(serverUrl, deviceToken, furgonId)
}
