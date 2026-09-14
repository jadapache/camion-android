package com.pronavicola.camion.ui

import com.pronavicola.camion.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass
import com.pronavicola.camion.CamionAppContext
import com.pronavicola.camion.data.local.entity.UsuarioEntity
import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.data.modbus.ModbusClient
import com.pronavicola.camion.data.modbus.toProcesoEstado
import com.pronavicola.camion.data.remote.FurgonInfo
import com.pronavicola.camion.data.repository.AppConfigRepository
import com.pronavicola.camion.data.repository.AuthRepository
import com.pronavicola.camion.data.repository.AuthResult
import com.pronavicola.camion.data.repository.DevicePairingRepository
import com.pronavicola.camion.data.repository.ParametrosRepository
import com.pronavicola.camion.data.repository.RegistrosRepository
import com.pronavicola.camion.domain.model.AppDestination
import com.pronavicola.camion.domain.model.HmiHistoryPoint
import com.pronavicola.camion.domain.model.HmiState
import com.pronavicola.camion.domain.model.ParametroUiModel
import com.pronavicola.camion.domain.model.ProcesoEstado
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.domain.model.ZoneStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

data class MainUiState(
    val currentUser: SessionUser? = null,
    val availableUsers: List<UsuarioEntity> = emptyList(),
    val hmiState: HmiState = HmiState(),
    val parametros: List<ParametroUiModel> = emptyList(),
    val registros: List<VariableRegistroEntity> = emptyList(),
    val statusMessage: String? = null,
    val loginPassword: String = "",
    val newPassword: String = "",
    val selectedUserId: String? = null,
    val showChangePassword: Boolean = false,
    val deviceId: String = "",
    val mqttBrokerUrl: String = "",
    val mqttUsername: String = "",
    val mqttPassword: String = "",
    val mqttTopicRoot: String = "furgones",
    val modbusHost: String = "192.168.1.10",
    val modbusPort: String = "502",
    val publishIntervalS: String = "5",
    val mqttConnected: Boolean = false,
    val modbusConnected: Boolean = false,
    val outboxPendingCount: Int = 0,
    val serverBaseUrl: String = "http://192.168.1.50:3000",
    val deviceToken: String = "",
    val pairingCode: String = "",
    val licenseStatus: String = "UNREGISTERED",
    val licenseKeyInput: String = "",
    val isPollingPairing: Boolean = false,
    val availableFurgones: List<FurgonInfo> = emptyList(),
)

class MainViewModel(
    private val authRepository: AuthRepository,
    private val parametrosRepository: ParametrosRepository,
    private val registrosRepository: RegistrosRepository,
    private val appConfigRepository: AppConfigRepository,
    private val modbusClient: ModbusClient,
    private val mqttClientManager: com.pronavicola.camion.data.mqtt.MqttClientManager,
    private val outboxQueueManager: com.pronavicola.camion.data.mqtt.OutboxQueueManager? = null,
    private val telemetryMqttCoordinator: com.pronavicola.camion.data.mqtt.TelemetryMqttCoordinator? = null,
    private val devicePairingRepository: DevicePairingRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val visibleDestinations: StateFlow<List<AppDestination>> = _uiState
        .map { state ->
            state.currentUser?.let { user ->
                AppDestination.entries.filter { dest ->
                    authRepository.canAccessScreen(user.idPerfil, dest.accessLevel)
                }
            } ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val user = authRepository.loadDefaultUser()

            // Load persistent configuration from AppConfigRepository
            val savedDeviceId = appConfigRepository.getDeviceId() ?: user.idVehiculo
            val savedBrokerUrl = appConfigRepository.getBrokerUrl() ?: com.pronavicola.camion.data.mqtt.MqttClientManager.DEFAULT_BROKER_URL
            val savedTopicRoot = appConfigRepository.getTopicRoot() ?: "furgones"
            val savedUsername = appConfigRepository.getBrokerUsername() ?: ""
            val savedPassword = appConfigRepository.getBrokerPassword() ?: ""
            val savedModbusHost = appConfigRepository.getModbusHost() ?: ModbusClient.DEFAULT_HOST
            val savedModbusPort = appConfigRepository.getModbusPort() ?: ModbusClient.DEFAULT_PORT.toString()
            val savedPublishInterval = appConfigRepository.getPublishInterval() ?: "5"
            val savedServerUrl = appConfigRepository.getServerBaseUrl() ?: "http://192.168.1.50:3000"
            val savedDeviceToken = appConfigRepository.getDeviceToken() ?: ""
            val savedPairingCode = appConfigRepository.getPairingCode() ?: ""
            val savedLicenseKey = appConfigRepository.getLicenseKey() ?: ""
            val savedLicenseStatus = appConfigRepository.getLicenseStatus() ?: "UNREGISTERED"

            _uiState.update { it.copy(
                currentUser = user,
                selectedUserId = user.idUsuario,
                deviceId = savedDeviceId,
                mqttBrokerUrl = savedBrokerUrl,
                mqttTopicRoot = savedTopicRoot,
                mqttUsername = savedUsername,
                mqttPassword = savedPassword,
                modbusHost = savedModbusHost,
                modbusPort = savedModbusPort,
                publishIntervalS = savedPublishInterval,
                serverBaseUrl = savedServerUrl,
                deviceToken = savedDeviceToken,
                pairingCode = savedPairingCode,
                licenseStatus = savedLicenseStatus,
                licenseKeyInput = savedLicenseKey,
            ) }

            telemetryMqttCoordinator?.updateDeviceId(savedDeviceId)
            telemetryMqttCoordinator?.updateTopicRoot(savedTopicRoot)
            savedPublishInterval.toLongOrNull()?.let {
                telemetryMqttCoordinator?.setPublishIntervalSeconds(it)
            }

            mqttClientManager.start(
                brokerUrl = savedBrokerUrl,
                clientId = "camion_$savedDeviceId",
                username = savedUsername,
                password = savedPassword,
            )

            val port = savedModbusPort.toIntOrNull() ?: ModbusClient.DEFAULT_PORT
            modbusClient.connect(host = savedModbusHost, port = port)
            modbusClient.iniciarProceso() // Auto-start simulation/process
        }

        // Observe MQTT connection state
        viewModelScope.launch {
            mqttClientManager.isConnected.collect { connected ->
                _uiState.update { it.copy(mqttConnected = connected) }
            }
        }

        // Observe Outbox queue pending count
        if (outboxQueueManager != null) {
            viewModelScope.launch {
                outboxQueueManager.pendingCount.collect { count ->
                    _uiState.update { it.copy(outboxPendingCount = count) }
                }
            }
        }

        viewModelScope.launch {
            authRepository.activeUsers.collect { users ->
                _uiState.update { it.copy(availableUsers = users) }
            }
        }

        viewModelScope.launch {
            combine(
                listOf(
                    modbusClient.isConnected,
                    modbusClient.temperatura1,
                    modbusClient.temperatura2,
                    modbusClient.humedad,
                    modbusClient.tension,
                    modbusClient.procesoActivo,
                    modbusClient.zonas,
                    modbusClient.estadoIrrigador,
                    modbusClient.damperActivo,
                    modbusClient.calefaccionActiva,
                    modbusClient.alarmasActivas,
                    _uiState.map { it.currentUser?.idVehiculo ?: "AAA000" }.distinctUntilChanged(),
                )
            ) { values ->
                val connected = values[0] as Boolean
                val t1 = values[1] as Double?
                val t2 = values[2] as Double?
                val hum = values[3] as Double?
                val ten = values[4] as Double?
                val proceso = values[5] as Boolean
                @Suppress("UNCHECKED_CAST")
                val zonas = values[6] as List<ZoneStatus>
                val irrigator = values[7] as ZoneStatus
                val damper = values[8] as Boolean
                val calefaccion = values[9] as Boolean
                @Suppress("UNCHECKED_CAST")
                val alarmas = values[10] as List<String>
                val vehiculo = values[11] as String

                val currentHistory = _uiState.value.hmiState.history
                val newHistory = if (proceso && t1 != null && t2 != null && hum != null) {
                    (currentHistory + HmiHistoryPoint(t1, t2, hum))
                        .takeLast(50) // Keep last 50 points
                } else if (!proceso) {
                    emptyList()
                } else {
                    currentHistory
                }

                HmiState(
                    idVehiculo = vehiculo,
                    conectado = connected,
                    temperatura1 = t1,
                    temperatura2 = t2,
                    humedad = hum,
                    tension = ten,
                    procesoEstado = proceso.toProcesoEstado(),
                    zonas = zonas,
                    estadoIrrigador = irrigator,
                    damperActivo = damper,
                    calefaccionActiva = calefaccion,
                    alarmasActivas = alarmas,
                    history = newHistory,
                )
            }.collect { hmi ->
                _uiState.update {
                    it.copy(
                        hmiState = hmi,
                        modbusConnected = hmi.conectado,
                    )
                }
            }
        }

        viewModelScope.launch {
            _uiState
                .map { it.currentUser }
                .distinctUntilChanged()
                .collectLatest { user ->
                    if (user == null) {
                        _uiState.update { it.copy(parametros = emptyList()) }
                        return@collectLatest
                    }
                    parametrosRepository.observeParametros(user).collect { base ->
                        val enriched = parametrosRepository.enrichWithPermissions(base, user)
                        _uiState.update { it.copy(parametros = enriched) }
                    }
                }
        }

        viewModelScope.launch {
            _uiState
                .map { it.currentUser }
                .distinctUntilChanged()
                .collectLatest { user ->
                    if (user == null) {
                        _uiState.update { it.copy(registros = emptyList()) }
                        return@collectLatest
                    }
                    registrosRepository.observeRegistros(user).collect { records ->
                        _uiState.update { it.copy(registros = records) }
                    }
                }
        }

        // Periodic data recording
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            while (true) {
                val state = _uiState.value
                val user = state.currentUser
                val hmi = state.hmiState

                if (user != null && hmi.procesoEstado == ProcesoEstado.ACTIVO) {
                    val registro = VariableRegistroEntity(
                        idVehiculo = user.idVehiculo.ifBlank { state.deviceId.ifBlank { "AAA000" } },
                        fecha = dateFormat.format(Date()),
                        temperatura1 = hmi.temperatura1 ?: 0.0,
                        temperatura2 = hmi.temperatura2 ?: 0.0,
                        humedad = hmi.humedad ?: 0.0,
                    )
                    registrosRepository.saveRegistro(registro)
                    // Enqueue to MQTT layer
                    telemetryMqttCoordinator?.enqueueVariableRegistro(registro)
                }
                delay(30000.milliseconds) // Record every 30 seconds
            }
        }
    }

    fun onLoginPasswordChange(value: String) {
        _uiState.update { it.copy(loginPassword = value) }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value) }
    }

    fun onUserSelected(idUsuario: String) {
        _uiState.update {
            it.copy(
                selectedUserId = idUsuario,
                loginPassword = "",
                newPassword = "",
                showChangePassword = false,
            )
        }
    }

    fun toggleChangePassword() {
        _uiState.update { it.copy(showChangePassword = !it.showChangePassword, newPassword = "") }
    }

    fun confirmLogin() {
        val state = _uiState.value
        val current = state.currentUser ?: return
        val targetId = state.selectedUserId ?: return

        viewModelScope.launch {
            if (state.showChangePassword) {
                when (
                    val result = authRepository.changePassword(
                        current,
                        state.loginPassword,
                        state.newPassword,
                    )
                ) {
                    is AuthResult.Success -> {
                        _uiState.update {
                            it.copy(
                                currentUser = result.user,
                                statusMessage = "Password cambiado correctamente",
                                loginPassword = "",
                                newPassword = "",
                                showChangePassword = false,
                            )
                        }
                    }
                    is AuthResult.Error -> showMessage(result.message)
                }
            } else {
                when (
                    val result = authRepository.switchUser(current, targetId, state.loginPassword)
                ) {
                    is AuthResult.Success -> {
                        _uiState.update {
                            it.copy(
                                currentUser = result.user,
                                selectedUserId = result.user.idUsuario,
                                statusMessage = "Usuario cambio a: ${result.user.usuario}",
                                loginPassword = "",
                                deviceId = result.user.idVehiculo,
                            )
                        }
                        result.user.idVehiculo.takeIf { it.isNotBlank() }?.let { telemetryMqttCoordinator?.updateDeviceId(it) }
                    }
                    is AuthResult.Error -> showMessage(result.message)
                }
            }
        }
    }

    fun iniciarProceso() {
        viewModelScope.launch {
            modbusClient.iniciarProceso()
            showMessage("Proceso iniciado")
        }
    }

    fun detenerProceso() {
        viewModelScope.launch {
            modbusClient.detenerProceso()
            showMessage("Proceso detenido")
        }
    }

    fun updateParametro(parametro: ParametroUiModel, nuevoValor: Double) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            parametrosRepository.updateParametro(user, parametro, nuevoValor)
                .onSuccess { showMessage("${parametro.nombre} actualizado") }
                .onFailure { showMessage(it.message ?: "Error al actualizar parametro") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun onDeviceIdChange(value: String) {
        _uiState.update { it.copy(deviceId = value) }
    }

    fun onMqttBrokerUrlChange(value: String) {
        _uiState.update { it.copy(mqttBrokerUrl = value) }
    }

    fun onMqttUsernameChange(value: String) {
        _uiState.update { it.copy(mqttUsername = value) }
    }

    fun onMqttPasswordChange(value: String) {
        _uiState.update { it.copy(mqttPassword = value) }
    }

    fun onMqttTopicRootChange(value: String) {
        _uiState.update { it.copy(mqttTopicRoot = value) }
    }

    fun onModbusHostChange(value: String) {
        _uiState.update { it.copy(modbusHost = value) }
    }

    fun onModbusPortChange(value: String) {
        _uiState.update { it.copy(modbusPort = value) }
    }

    fun onPublishIntervalChange(value: String) {
        _uiState.update { it.copy(publishIntervalS = value) }
    }

    fun onServerBaseUrlChange(value: String) {
        _uiState.update { it.copy(serverBaseUrl = value) }
    }

    fun onLicenseKeyInputChange(value: String) {
        _uiState.update { it.copy(licenseKeyInput = value) }
    }

    fun requestPairingCode() {
        val repo = devicePairingRepository ?: return
        val state = _uiState.value
        viewModelScope.launch {
            repo.requestPairingCode(state.serverBaseUrl)
                .onSuccess { (devId, code) ->
                    _uiState.update {
                        it.copy(
                            deviceId = devId,
                            pairingCode = code,
                            licenseStatus = "PENDING_PAIRING",
                            statusMessage = "Código PIN $code generado. Ingrese el PIN en la web FURGN BI.",
                        )
                    }
                    startPairingCheckLoop()
                }
                .onFailure {
                    showMessage("Error al solicitar PIN: ${it.message}")
                }
        }
    }

    fun checkPairingStatus() {
        val repo = devicePairingRepository ?: return
        val state = _uiState.value
        if (state.pairingCode.isBlank() || state.deviceId.isBlank()) return

        viewModelScope.launch {
            repo.checkPairingStatus(state.serverBaseUrl, state.pairingCode, state.deviceId)
                .onSuccess { isPaired ->
                    if (isPaired) {
                        val token = appConfigRepository.getDeviceToken() ?: ""
                        val newDevId = appConfigRepository.getDeviceId() ?: state.deviceId
                        val newBroker = appConfigRepository.getBrokerUrl() ?: state.mqttBrokerUrl
                        val newTopic = appConfigRepository.getTopicRoot() ?: state.mqttTopicRoot

                        _uiState.update {
                            it.copy(
                                deviceToken = token,
                                deviceId = newDevId,
                                mqttBrokerUrl = newBroker,
                                mqttTopicRoot = newTopic,
                                licenseStatus = "ACTIVE",
                                statusMessage = "¡Dispositivo vinculado y activo correctamente!",
                            )
                        }
                    } else {
                        showMessage("Estado de vinculación: ${state.licenseStatus}")
                    }
                }
                .onFailure {
                    showMessage("Error consultando estado: ${it.message}")
                }
        }
    }

    fun activateLicenseKey() {
        val repo = devicePairingRepository ?: return
        val state = _uiState.value
        if (state.licenseKeyInput.isBlank()) {
            showMessage("Ingrese una clave de licencia válida")
            return
        }

        viewModelScope.launch {
            repo.activateLicenseKey(state.serverBaseUrl, state.licenseKeyInput)
                .onSuccess { isActivated ->
                    if (isActivated) {
                        val token = appConfigRepository.getDeviceToken() ?: ""
                        val newDevId = appConfigRepository.getDeviceId() ?: state.deviceId
                        val newBroker = appConfigRepository.getBrokerUrl() ?: state.mqttBrokerUrl
                        val newTopic = appConfigRepository.getTopicRoot() ?: state.mqttTopicRoot

                        _uiState.update {
                            it.copy(
                                deviceToken = token,
                                deviceId = newDevId,
                                mqttBrokerUrl = newBroker,
                                mqttTopicRoot = newTopic,
                                licenseStatus = "ACTIVE",
                                statusMessage = "¡Licencia activada con éxito!",
                            )
                        }
                    } else {
                        showMessage("Error activando clave de licencia")
                    }
                }
                .onFailure {
                    showMessage("Error al activar clave: ${it.message}")
                }
        }
    }

    fun fetchAvailableFurgones() {
        val repo = devicePairingRepository ?: return
        val state = _uiState.value
        viewModelScope.launch {
            repo.getAvailableFurgones(state.serverBaseUrl, state.deviceToken)
                .onSuccess { list ->
                    _uiState.update { it.copy(availableFurgones = list) }
                }
                .onFailure {
                    Log.w("MainViewModel", "Could not fetch furgones list: ${it.message}")
                }
        }
    }

    fun selectAndAssignFurgon(furgonId: String) {
        val repo = devicePairingRepository ?: return
        val state = _uiState.value
        viewModelScope.launch {
            repo.updateAssociatedFurgon(state.serverBaseUrl, state.deviceToken, furgonId)
                .onSuccess { newPlate ->
                    _uiState.update { it.copy(deviceId = newPlate, statusMessage = "Placa de furgón asociada: $newPlate") }
                    telemetryMqttCoordinator?.updateDeviceId(newPlate)
                }
                .onFailure {
                    showMessage("Error al asociar furgón: ${it.message}")
                }
        }
    }

    private fun startPairingCheckLoop() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPollingPairing = true) }
            var attempts = 0
            while (_uiState.value.isPollingPairing && attempts < 120 && _uiState.value.licenseStatus != "ACTIVE") {
                checkPairingStatus()
                delay(5000.milliseconds)
                attempts++
            }
            _uiState.update { it.copy(isPollingPairing = false) }
        }
    }

    fun saveSystemSettings() {
        val state = _uiState.value
        val user = state.currentUser ?: return
        viewModelScope.launch {
            // Save to database
            authRepository.updateDeviceId(user.idUsuario, state.deviceId)
            appConfigRepository.saveDeviceId(state.deviceId)
            appConfigRepository.saveBrokerUrl(state.mqttBrokerUrl)
            appConfigRepository.saveTopicRoot(state.mqttTopicRoot)
            appConfigRepository.saveBrokerUsername(state.mqttUsername)
            appConfigRepository.saveBrokerPassword(state.mqttPassword)
            appConfigRepository.saveModbusHost(state.modbusHost)
            appConfigRepository.saveModbusPort(state.modbusPort)
            appConfigRepository.savePublishInterval(state.publishIntervalS)
            appConfigRepository.saveServerBaseUrl(state.serverBaseUrl)

            telemetryMqttCoordinator?.updateDeviceId(state.deviceId)
            telemetryMqttCoordinator?.updateTopicRoot(state.mqttTopicRoot)
            state.publishIntervalS.toLongOrNull()?.let {
                telemetryMqttCoordinator?.setPublishIntervalSeconds(it)
            }

            // Restart MQTT client with new parameters
            mqttClientManager.start(
                brokerUrl = state.mqttBrokerUrl,
                clientId = "camion_${state.deviceId}",
                username = state.mqttUsername,
                password = state.mqttPassword,
            )

            // Reconnect Modbus TCP client if host/port changed
            val port = state.modbusPort.toIntOrNull() ?: ModbusClient.DEFAULT_PORT
            modbusClient.connect(host = state.modbusHost, port = port)

            _uiState.update { it.copy(
                currentUser = user.copy(idVehiculo = state.deviceId),
                statusMessage = "Configuración de sistema guardada correctamente",
            ) }
        }
    }

    fun syncAllRegistros() {
        val state = _uiState.value
        viewModelScope.launch {
            val registros = state.registros
            if (registros.isEmpty()) {
                showMessage("No hay registros almacenados para sincronizar")
                return@launch
            }
            telemetryMqttCoordinator?.syncAllVariableRegistros(registros)
            showMessage("Enviando ${registros.size} registros a la cola MQTT...")
        }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(statusMessage = message) }
    }

    class Factory(private val appContext: CamionAppContext) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
            return MainViewModel(
                appContext.authRepository,
                appContext.parametrosRepository,
                appContext.registrosRepository,
                appContext.appConfigRepository,
                appContext.modbusClient,
                appContext.mqttClientManager,
                appContext.outboxQueueManager,
                appContext.telemetryMqttCoordinator,
                appContext.devicePairingRepository,
            ) as T
        }
    }
}
