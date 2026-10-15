package com.pronavicola.camion.presentation.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronavicola.camion.domain.repository.AppConfigRepository
import com.pronavicola.camion.domain.usecase.config.AppConfigData
import com.pronavicola.camion.domain.usecase.config.GetAppConfigUseCase
import com.pronavicola.camion.domain.usecase.config.PairDeviceUseCase
import com.pronavicola.camion.domain.usecase.config.RestartMqttClientUseCase
import com.pronavicola.camion.domain.usecase.config.SaveAppConfigUseCase
import com.pronavicola.camion.domain.usecase.config.TestMqttConnectionUseCase
import com.pronavicola.camion.domain.usecase.config.UpdateTruckPlateUseCase
import com.pronavicola.camion.domain.usecase.hmi.ConnectModbusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConfigUiState(
    val configData: AppConfigData? = null,
    val brokerUrl: String = "",
    val brokerUsername: String = "",
    val brokerPassword: String = "",
    val isCredentialsExpanded: Boolean = false,
    val modbusHost: String = "",
    val modbusPort: String = "",
    val publishInterval: String = "30",
    val deviceId: String = "",
    val furgonPlate: String = "ABC123",
    val topicRoot: String = "furgones",
    val serverBaseUrl: String = "",
    val pairingCode: String = "",
    val licenseKey: String = "",
    val licenseStatus: String = "UNPAIRED",
    val isTestingMqtt: Boolean = false,
    val testMqttResult: String? = null,
    val testMqttPayloadJson: String? = null,
    val testMqttSuccess: Boolean? = null,
    val isPairing: Boolean = false,
    val pairingError: String? = null,
    val pairingSuccess: String? = null,
    val saveMessage: String? = null,
    val availableFurgones: List<com.pronavicola.camion.data.remote.FurgonInfo> = emptyList(),
)

class ConfigViewModel(
    private val getAppConfigUseCase: GetAppConfigUseCase,
    private val saveAppConfigUseCase: SaveAppConfigUseCase,
    private val testMqttConnectionUseCase: TestMqttConnectionUseCase,
    private val restartMqttClientUseCase: RestartMqttClientUseCase,
    private val pairDeviceUseCase: PairDeviceUseCase,
    private val connectModbusUseCase: ConnectModbusUseCase,
    private val updateTruckPlateUseCase: UpdateTruckPlateUseCase,
    private val appConfigRepository: AppConfigRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    init {
        loadConfig()
        if (appConfigRepository != null) {
            viewModelScope.launch {
                appConfigRepository.observeLicenseStatus().collect { status ->
                    if (!status.isNullOrBlank()) {
                        _uiState.update { it.copy(licenseStatus = status) }
                    }
                }
            }
            viewModelScope.launch {
                appConfigRepository.observeFurgonPlate().collect { plate ->
                    if (!plate.isNullOrBlank() && plate != _uiState.value.deviceId) {
                        _uiState.update { it.copy(furgonPlate = plate) }
                    }
                }
            }
        }
    }

    fun loadConfig() {
        viewModelScope.launch {
            val data = getAppConfigUseCase()
            val effectiveDeviceId = if (data.deviceId.isNotBlank() && data.deviceId != "ABC123") {
                data.deviceId
            } else {
                pairDeviceUseCase.getHardwareFingerprint()
            }
            val sanitizedPlate = if (data.furgonPlate.isNotBlank() && data.furgonPlate != data.deviceId) {
                data.furgonPlate
            } else {
                "ABC123"
            }
            val rawBroker = data.brokerUrl
            val displayBroker = when {
                rawBroker.startsWith("tcp://", ignoreCase = true) -> "mqtt://" + rawBroker.removePrefix("tcp://")
                rawBroker.startsWith("ssl://", ignoreCase = true) -> "mqtts://" + rawBroker.removePrefix("ssl://")
                else -> rawBroker
            }
            _uiState.update {
                it.copy(
                    configData = data,
                    brokerUrl = displayBroker,
                    brokerUsername = data.brokerUsername,
                    brokerPassword = data.brokerPassword,
                    topicRoot = data.topicRoot.ifBlank { "furgones" },
                    modbusHost = data.modbusHost,
                    modbusPort = data.modbusPort,
                    publishInterval = data.publishInterval,
                    deviceId = effectiveDeviceId,
                    furgonPlate = sanitizedPlate,
                    serverBaseUrl = data.serverBaseUrl,
                    pairingCode = data.pairingCode ?: "",
                    licenseKey = data.licenseKey ?: "",
                    licenseStatus = data.licenseStatus ?: "UNPAIRED",
                )
            }
        }
    }

    fun onBrokerUrlChange(url: String) = _uiState.update { it.copy(brokerUrl = url) }
    fun onBrokerUsernameChange(user: String) = _uiState.update { it.copy(brokerUsername = user) }
    fun onBrokerPasswordChange(pass: String) = _uiState.update { it.copy(brokerPassword = pass) }
    fun onTopicRootChange(root: String) = _uiState.update { it.copy(topicRoot = root) }
    fun toggleCredentialsExpanded() = _uiState.update { it.copy(isCredentialsExpanded = !it.isCredentialsExpanded) }
    fun onModbusHostChange(host: String) = _uiState.update { it.copy(modbusHost = host) }
    fun onModbusPortChange(port: String) = _uiState.update { it.copy(modbusPort = port) }
    fun onPublishIntervalChange(interval: String) = _uiState.update { it.copy(publishInterval = interval) }
    fun onServerBaseUrlChange(url: String) = _uiState.update { it.copy(serverBaseUrl = url) }
    fun onLicenseKeyInputChange(key: String) = _uiState.update { it.copy(licenseKey = key) }
    fun onDeviceIdChange(id: String) = _uiState.update { it.copy(deviceId = id) }
    fun onFurgonPlateChange(plate: String) = _uiState.update { it.copy(furgonPlate = plate) }
    fun updateAssignedPlate(plate: String) = _uiState.update { it.copy(furgonPlate = plate) }

    fun saveSystemSettings() {
        viewModelScope.launch {
            val devIdToSave = _uiState.value.deviceId.trim()
            val plateToSave = _uiState.value.furgonPlate.trim()
            val interval = _uiState.value.publishInterval
            val host = _uiState.value.modbusHost
            val portStr = _uiState.value.modbusPort
            val serverUrl = _uiState.value.serverBaseUrl
            val licenseKey = _uiState.value.licenseKey

            saveAppConfigUseCase(
                publishInterval = interval,
                deviceId = devIdToSave.ifBlank { null },
                furgonPlate = plateToSave.ifBlank { null },
                modbusHost = host,
                modbusPort = portStr,
                serverBaseUrl = serverUrl,
                licenseKey = licenseKey,
            )

            portStr.toIntOrNull()?.let { p ->
                connectModbusUseCase(host, p)
            }

            if (plateToSave.isNotBlank()) {
                val result = updateTruckPlateUseCase(plateToSave)
                if (result.isSuccess) {
                    val vehiculo = result.getOrThrow()
                    _uiState.update {
                        it.copy(
                            furgonPlate = vehiculo.placa,
                            saveMessage = "Ajustes guardados.",
                        )
                    }
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Error al registrar la placa del vehículo"
                    _uiState.update {
                        it.copy(saveMessage = "Error: $err")
                    }
                }
            } else {
                _uiState.update { it.copy(saveMessage = "Ajustes guardados.") }
            }
        }
    }

    fun saveMqttConfig() {
        viewModelScope.launch {
            val broker = _uiState.value.brokerUrl.trim()
            val formattedBroker = when {
                broker.startsWith("tcp://", ignoreCase = true) -> "mqtt://" + broker.removePrefix("tcp://")
                broker.startsWith("ssl://", ignoreCase = true) -> "mqtts://" + broker.removePrefix("ssl://")
                else -> broker
            }
            saveAppConfigUseCase(
                brokerUrl = formattedBroker,
                brokerUsername = _uiState.value.brokerUsername,
                brokerPassword = _uiState.value.brokerPassword,
                publishInterval = _uiState.value.publishInterval,
                topicRoot = _uiState.value.topicRoot,
            )
            restartMqttClientUseCase()
            _uiState.update { it.copy(brokerUrl = formattedBroker, saveMessage = "Configuración MQTT guardada y conexión reiniciada") }
        }
    }

    fun saveModbusConfig() {
        viewModelScope.launch {
            val host = _uiState.value.modbusHost
            val portStr = _uiState.value.modbusPort
            saveAppConfigUseCase(
                modbusHost = host,
                modbusPort = portStr,
            )
            portStr.toIntOrNull()?.let { p ->
                connectModbusUseCase(host, p)
            }
            _uiState.update { it.copy(saveMessage = "Configuración Modbus guardada y reconexión solicitada") }
        }
    }

    fun testMqttConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingMqtt = true, testMqttResult = null, testMqttPayloadJson = null, testMqttSuccess = null) }
            val current = _uiState.value
            val targetDevId = current.deviceId.trim().ifBlank { pairDeviceUseCase.getHardwareFingerprint() }
            val result = testMqttConnectionUseCase(
                brokerUrl = current.brokerUrl,
                username = current.brokerUsername,
                password = current.brokerPassword,
                deviceId = targetDevId
            )
            if (result.isSuccess) {
                val data = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        isTestingMqtt = false,
                        testMqttResult = data.message,
                        testMqttPayloadJson = data.payloadJson,
                        testMqttSuccess = true,
                    )
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Error al conectar con el broker"
                _uiState.update {
                    it.copy(
                        isTestingMqtt = false,
                        testMqttResult = errorMsg,
                        testMqttPayloadJson = null,
                        testMqttSuccess = false,
                    )
                }
            }
        }
    }

    fun requestPairingCode() {
        viewModelScope.launch {
            val serverUrl = _uiState.value.serverBaseUrl.trim()
            if (serverUrl.isBlank()) {
                _uiState.update { it.copy(pairingError = "Debe ingresar la URL del servidor backend REST antes de solicitar un PIN.", isPairing = false) }
                return@launch
            }
            _uiState.update { it.copy(isPairing = true, pairingError = null, pairingSuccess = null) }
            val result = pairDeviceUseCase.requestPairingCode(serverUrl)
            if (result.isSuccess) {
                val (devId, code) = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        isPairing = false,
                        deviceId = devId,
                        pairingCode = code,
                        licenseStatus = "PENDING_PAIRING",
                        pairingSuccess = "Código PIN solicitado: $code",
                        pairingError = null,
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Error desconocido"
                _uiState.update {
                    it.copy(
                        isPairing = false,
                        pairingError = "Error al solicitar código PIN: $err",
                        pairingSuccess = null,
                    )
                }
            }
        }
    }

    fun checkPairingStatus() {
        viewModelScope.launch {
            val serverUrl = _uiState.value.serverBaseUrl.trim()
            if (serverUrl.isBlank()) {
                _uiState.update { it.copy(pairingError = "Debe ingresar la URL del servidor backend REST antes de verificar.", isPairing = false) }
                return@launch
            }
            val code = _uiState.value.pairingCode.trim()
            val devId = _uiState.value.deviceId.trim().ifBlank { pairDeviceUseCase.getHardwareFingerprint() }

            _uiState.update { it.copy(isPairing = true, pairingError = null, pairingSuccess = null) }
            val result = pairDeviceUseCase.checkPairingStatus(serverUrl, code, devId)
            if (result.isSuccess) {
                val active = result.getOrThrow()
                if (active) {
                    _uiState.update {
                        it.copy(
                            isPairing = false,
                            licenseStatus = "ACTIVE",
                            pairingSuccess = "¡Dispositivo vinculado y activo correctamente!",
                            pairingError = null,
                        )
                    }
                    val freshConfig = getAppConfigUseCase()
                    val savedPlate = freshConfig.furgonPlate
                    val finalPlate = if (savedPlate.isNotBlank() && savedPlate != freshConfig.deviceId) savedPlate else "ABC123"
                    updateTruckPlateUseCase(finalPlate)
                    loadConfig()
                } else {
                    val hasPendingCode = code.isNotBlank()
                    _uiState.update {
                        it.copy(
                            isPairing = false,
                            licenseStatus = if (hasPendingCode) "PENDING_PAIRING" else "UNPAIRED",
                            pairingSuccess = if (hasPendingCode) "Conexión exitosa con la API. Pendiente de aprobación/asignación en la plataforma web." else null,
                            pairingError = if (!hasPendingCode) "No se encontró registro previo activo para el dispositivo en FURGN BI. Solicite un PIN o active con la clave de registro." else null,
                        )
                    }
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Error desconocido"
                val displayMsg = if (err.contains("No se encontró registro previo", ignoreCase = true) || err.contains("404")) {
                    "No se encontró registro previo activo en FURGN BI. Solicite un PIN o active con la clave de registro."
                } else if (err.startsWith("Error de conexión con la API")) {
                    err
                } else {
                    "Error de conexión con la API: $err"
                }
                _uiState.update {
                    it.copy(
                        isPairing = false,
                        pairingError = displayMsg,
                        pairingSuccess = null,
                    )
                }
            }
        }
    }

    fun activateLicenseKey() {
        viewModelScope.launch {
            val serverUrl = _uiState.value.serverBaseUrl.trim()
            if (serverUrl.isBlank()) {
                _uiState.update { it.copy(pairingError = "Debe ingresar la URL del servidor backend REST antes de activar.", isPairing = false) }
                return@launch
            }
            val key = _uiState.value.licenseKey.trim()
            if (key.isBlank()) {
                _uiState.update { it.copy(pairingError = "Ingrese una clave de registro válida.", isPairing = false) }
                return@launch
            }

            _uiState.update { it.copy(isPairing = true, pairingError = null, pairingSuccess = null) }
            val result = pairDeviceUseCase.activateLicenseKey(serverUrl, key)
            if (result.isSuccess) {
                val active = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        isPairing = false,
                        licenseStatus = if (active) "ACTIVE" else "UNPAIRED",
                        pairingSuccess = if (active) "¡Licencia activada exitosamente!" else "Clave no válida o rechazada por el servidor",
                        pairingError = null,
                    )
                }
                if (active) {
                    val freshConfig = getAppConfigUseCase()
                    val savedPlate = freshConfig.furgonPlate
                    val finalPlate = if (savedPlate.isNotBlank() && savedPlate != freshConfig.deviceId) savedPlate else "ABC123"
                    updateTruckPlateUseCase(finalPlate)
                    loadConfig()
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Error al conectar con la API"
                _uiState.update {
                    it.copy(
                        isPairing = false,
                        pairingError = "Error al activar clave de licencia: $err",
                        pairingSuccess = null,
                    )
                }
            }
        }
    }

    fun unlinkDevice() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPairing = true, pairingError = null, pairingSuccess = null) }
            val serverUrl = _uiState.value.serverBaseUrl.trim()
            val result = pairDeviceUseCase.unpairDevice(serverUrl, null)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isPairing = false,
                        licenseStatus = "UNPAIRED",
                        pairingCode = "",
                        licenseKey = "",
                        pairingSuccess = "Dispositivo desvinculado con éxito.",
                        pairingError = null,
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Error al desvincular"
                _uiState.update {
                    it.copy(
                        isPairing = false,
                        pairingError = "Error al desvincular dispositivo: $err",
                        pairingSuccess = null,
                    )
                }
            }
        }
    }

    fun fetchAvailableFurgones() {
        viewModelScope.launch {
            val serverUrl = _uiState.value.serverBaseUrl
            val token = _uiState.value.configData?.deviceToken
            val result = pairDeviceUseCase.getAvailableFurgones(serverUrl, token)
            if (result.isSuccess) {
                val list = result.getOrThrow()
                _uiState.update { it.copy(availableFurgones = list) }
            }
        }
    }

    fun updateAssociatedFurgon(furgon: com.pronavicola.camion.data.remote.FurgonInfo) {
        viewModelScope.launch {
            val serverUrl = _uiState.value.serverBaseUrl
            val token = _uiState.value.configData?.deviceToken
            val result = pairDeviceUseCase.updateAssociatedFurgon(serverUrl, token, furgon.id)
            if (result.isSuccess) {
                val plate = result.getOrThrow()
                _uiState.update { it.copy(furgonPlate = plate, saveMessage = "Furgón asignado: $plate") }
                loadConfig()
            } else {
                _uiState.update { it.copy(pairingError = "Error asignando furgón: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun clearSaveMessage() {
        _uiState.update { it.copy(saveMessage = null) }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                saveMessage = null,
                testMqttResult = null,
                testMqttPayloadJson = null,
                testMqttSuccess = null,
                pairingError = null,
                pairingSuccess = null,
            )
        }
    }
}
