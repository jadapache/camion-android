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
            deviceId = appConfigRepository.getDeviceId() ?: "CAM01",
            furgonPlate = appConfigRepository.getFurgonPlate() ?: "AAA000",
            brokerUrl = appConfigRepository.getBrokerUrl() ?: "tcp://broker.emqx.io:1883",
            topicRoot = appConfigRepository.getTopicRoot() ?: "furgones",
            brokerUsername = appConfigRepository.getBrokerUsername() ?: "",
            brokerPassword = appConfigRepository.getBrokerPassword() ?: "",
            modbusHost = appConfigRepository.getModbusHost() ?: "127.0.0.1",
            modbusPort = appConfigRepository.getModbusPort() ?: "502",
            publishInterval = appConfigRepository.getPublishInterval() ?: "5",
            serverBaseUrl = appConfigRepository.getServerBaseUrl() ?: "https://screens-api.pronavicola.com",
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

class TestMqttConnectionUseCase(
    private val mqttRepository: MqttRepository,
    private val appConfigRepository: AppConfigRepository,
) {
    suspend operator fun invoke(): Result<String> = withContext(Dispatchers.IO) {
        val brokerUrl = appConfigRepository.getBrokerUrl() ?: "tcp://broker.emqx.io:1883"
        val deviceId = appConfigRepository.getDeviceId() ?: "CAM01"
        val username = appConfigRepository.getBrokerUsername() ?: ""
        val password = appConfigRepository.getBrokerPassword() ?: ""

        val clientId = "camion_test_${System.currentTimeMillis() % 10000}"
        mqttRepository.start(brokerUrl, clientId, username, password)

        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())

        val telemetryTopic = "furgones/$deviceId/telemetry"
        val locationTopic = "furgones/$deviceId/location"
        val alarmTopic = "furgones/$deviceId/alarms"

        val telemetryPayload = """{"device_id":"$deviceId","timestamp":"$timestamp","test":true,"temperatura_1":21.5,"temperatura_2":22.0,"humedad":65.0,"tension":24.0}"""
        val locationPayload = """{"device_id":"$deviceId","timestamp":"$timestamp","test":true,"latitude":4.7110,"longitude":-74.0721,"speed":0.0}"""
        val alarmPayload = """{"device_id":"$deviceId","timestamp":"$timestamp","test":true,"alarms":[],"status":"NORMAL"}"""

        val tSent = mqttRepository.publish(telemetryTopic, telemetryPayload, 1)
        val lSent = mqttRepository.publish(locationTopic, locationPayload, 1)
        val aSent = mqttRepository.publish(alarmTopic, alarmPayload, 1)

        if (tSent && lSent && aSent) {
            Result.success("Conectado con éxito. Tramas de prueba enviadas a los 3 tópicos (telemetry, location, alarms).")
        } else if (tSent || lSent || aSent) {
            Result.success("Conectado. Enviados parcialmente: telemetry=$tSent, location=$lSent, alarms=$aSent")
        } else {
            Result.failure(Exception("No se pudo conectar al broker MQTT en $brokerUrl"))
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
}
