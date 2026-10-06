package com.pronavicola.camion.presentation.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val deviceId: String = "ABC123",
    val furgonPlate: String = "ABC123",
    val serverBaseUrl: String = "",
    val pairingCode: String = "",
    val licenseStatus: String = "UNPAIRED",
    val isTestingMqtt: Boolean = false,
    val testMqttResult: String? = null,
    val testMqttPayloadJson: String? = null,
    val testMqttSuccess: Boolean? = null,
    val isPairing: Boolean = false,
    val pairingError: String? = null,
    val pairingSuccess: String? = null,
    val saveMessage: String? = null,
)

class ConfigViewModel(
    private val getAppConfigUseCase: GetAppConfigUseCase,
    private val saveAppConfigUseCase: SaveAppConfigUseCase,
    private val testMqttConnectionUseCase: TestMqttConnectionUseCase,
    private val restartMqttClientUseCase: RestartMqttClientUseCase,
    private val pairDeviceUseCase: PairDeviceUseCase,
    private val connectModbusUseCase: ConnectModbusUseCase,
    private val updateTruckPlateUseCase: UpdateTruckPlateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    init {
        loadConfig()
    }

    fun loadConfig() {
        viewModelScope.launch {
            val data = getAppConfigUseCase()
            _uiState.update {
                it.copy(
                    configData = data,
                    brokerUrl = data.brokerUrl,
                    brokerUsername = data.brokerUsername,
                    brokerPassword = data.brokerPassword,
                    modbusHost = data.modbusHost,
                    modbusPort = data.modbusPort,
                    publishInterval = data.publishInterval,
                    deviceId = data.deviceId,
                    furgonPlate = data.furgonPlate,
                    serverBaseUrl = data.serverBaseUrl,
                    pairingCode = data.pairingCode ?: "",
                    licenseStatus = data.licenseStatus ?: "UNPAIRED",
                )
            }
        }
    }

    fun onBrokerUrlChange(url: String) = _uiState.update { it.copy(brokerUrl = url) }
    fun onBrokerUsernameChange(user: String) = _uiState.update { it.copy(brokerUsername = user) }
    fun onBrokerPasswordChange(pass: String) = _uiState.update { it.copy(brokerPassword = pass) }
    fun toggleCredentialsExpanded() = _uiState.update { it.copy(isCredentialsExpanded = !it.isCredentialsExpanded) }
    fun onModbusHostChange(host: String) = _uiState.update { it.copy(modbusHost = host) }
    fun onModbusPortChange(port: String) = _uiState.update { it.copy(modbusPort = port) }
    fun onPublishIntervalChange(interval: String) = _uiState.update { it.copy(publishInterval = interval) }
    fun onDeviceIdChange(id: String) = _uiState.update { it.copy(deviceId = id, furgonPlate = id) }
    fun onFurgonPlateChange(plate: String) = _uiState.update { it.copy(furgonPlate = plate, deviceId = plate) }

    fun saveSystemSettings() {
        viewModelScope.launch {
            val plateToSave = _uiState.value.deviceId.ifBlank { _uiState.value.furgonPlate }.trim()
            val interval = _uiState.value.publishInterval
            val host = _uiState.value.modbusHost
            val portStr = _uiState.value.modbusPort

            saveAppConfigUseCase(
                publishInterval = interval,
                deviceId = plateToSave,
                furgonPlate = plateToSave,
                modbusHost = host,
                modbusPort = portStr,
            )

            portStr.toIntOrNull()?.let { p ->
                connectModbusUseCase(host, p)
            }

            val result = updateTruckPlateUseCase(plateToSave)
            if (result.isSuccess) {
                val vehiculo = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        deviceId = vehiculo.placa,
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
        }
    }

    fun saveMqttConfig() {
        viewModelScope.launch {
            saveAppConfigUseCase(
                brokerUrl = _uiState.value.brokerUrl,
                brokerUsername = _uiState.value.brokerUsername,
                brokerPassword = _uiState.value.brokerPassword,
                publishInterval = _uiState.value.publishInterval,
            )
            restartMqttClientUseCase()
            _uiState.update { it.copy(saveMessage = "Configuración MQTT guardada y conexión reiniciada") }
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
            val result = testMqttConnectionUseCase(
                brokerUrl = current.brokerUrl,
                username = current.brokerUsername,
                password = current.brokerPassword,
                deviceId = current.deviceId.ifBlank { current.furgonPlate }.ifBlank { "ABC123" }
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
            _uiState.update { it.copy(isPairing = true, pairingError = null, pairingSuccess = null) }
            val serverUrl = _uiState.value.serverBaseUrl
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
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isPairing = false,
                        pairingError = result.exceptionOrNull()?.message ?: "Error al solicitar código PIN",
                    )
                }
            }
        }
    }

    fun checkPairingStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPairing = true, pairingError = null, pairingSuccess = null) }
            val serverUrl = _uiState.value.serverBaseUrl
            val code = _uiState.value.pairingCode
            val devId = _uiState.value.deviceId
            val result = pairDeviceUseCase.checkPairingStatus(serverUrl, code, devId)
            if (result.isSuccess) {
                val active = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        isPairing = false,
                        licenseStatus = if (active) "ACTIVE" else "PENDING_PAIRING",
                        pairingSuccess = if (active) "¡Dispositivo emparejado y activo!" else "Pendiente de aprobación en el servidor",
                    )
                }
                if (active) loadConfig()
            } else {
                _uiState.update {
                    it.copy(
                        isPairing = false,
                        pairingError = result.exceptionOrNull()?.message ?: "Error al verificar estado",
                    )
                }
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
