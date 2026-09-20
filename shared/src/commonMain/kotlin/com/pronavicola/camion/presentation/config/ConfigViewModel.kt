package com.pronavicola.camion.presentation.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronavicola.camion.domain.usecase.config.AppConfigData
import com.pronavicola.camion.domain.usecase.config.GetAppConfigUseCase
import com.pronavicola.camion.domain.usecase.config.PairDeviceUseCase
import com.pronavicola.camion.domain.usecase.config.SaveAppConfigUseCase
import com.pronavicola.camion.domain.usecase.config.TestMqttConnectionUseCase
import com.pronavicola.camion.domain.usecase.hmi.ConnectModbusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConfigUiState(
    val configData: AppConfigData? = null,
    val brokerUrl: String = "tcp://broker.emqx.io:1883",
    val brokerUsername: String = "",
    val brokerPassword: String = "",
    val isCredentialsExpanded: Boolean = false,
    val modbusHost: String = "127.0.0.1",
    val modbusPort: String = "502",
    val publishInterval: String = "5",
    val deviceId: String = "CAM01",
    val furgonPlate: String = "AAA000",
    val serverBaseUrl: String = "https://screens-api.pronavicola.com",
    val pairingCode: String = "",
    val licenseStatus: String = "UNPAIRED",
    val isTestingMqtt: Boolean = false,
    val testMqttResult: String? = null,
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
    private val pairDeviceUseCase: PairDeviceUseCase,
    private val connectModbusUseCase: ConnectModbusUseCase,
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
    fun onDeviceIdChange(id: String) = _uiState.update { it.copy(deviceId = id) }
    fun onFurgonPlateChange(plate: String) = _uiState.update { it.copy(furgonPlate = plate) }

    fun saveMqttConfig() {
        viewModelScope.launch {
            saveAppConfigUseCase(
                brokerUrl = _uiState.value.brokerUrl,
                brokerUsername = _uiState.value.brokerUsername,
                brokerPassword = _uiState.value.brokerPassword,
                publishInterval = _uiState.value.publishInterval,
            )
            _uiState.update { it.copy(saveMessage = "Configuración MQTT guardada exitosamente") }
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
            _uiState.update { it.copy(isTestingMqtt = true, testMqttResult = null, testMqttSuccess = null) }
            val result = testMqttConnectionUseCase()
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isTestingMqtt = false,
                        testMqttResult = result.getOrNull(),
                        testMqttSuccess = true,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isTestingMqtt = false,
                        testMqttResult = result.exceptionOrNull()?.message ?: "Error al conectar con el broker",
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

    fun clearMessages() {
        _uiState.update {
            it.copy(
                saveMessage = null,
                testMqttResult = null,
                testMqttSuccess = null,
                pairingError = null,
                pairingSuccess = null,
            )
        }
    }
}
