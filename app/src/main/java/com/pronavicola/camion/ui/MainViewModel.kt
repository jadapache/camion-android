package com.pronavicola.camion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pronavicola.camion.CamionApplication
import com.pronavicola.camion.data.local.entity.UsuarioEntity
import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.domain.model.ZoneStatus
import com.pronavicola.camion.data.modbus.ModbusClient
import com.pronavicola.camion.data.modbus.toProcesoEstado
import com.pronavicola.camion.data.repository.AuthRepository
import com.pronavicola.camion.data.repository.AuthResult
import com.pronavicola.camion.data.repository.ParametrosRepository
import com.pronavicola.camion.data.repository.RegistrosRepository
import com.pronavicola.camion.domain.model.AppDestination
import com.pronavicola.camion.domain.model.HmiState
import com.pronavicola.camion.domain.model.ParametroUiModel
import com.pronavicola.camion.domain.model.SessionUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
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
)

class MainViewModel(
    private val authRepository: AuthRepository,
    private val parametrosRepository: ParametrosRepository,
    private val registrosRepository: RegistrosRepository,
    private val modbusClient: ModbusClient,
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
            _uiState.update { it.copy(currentUser = user, selectedUserId = user.idUsuario) }
            modbusClient.connect()
            modbusClient.iniciarProceso() // Auto-start simulation/process
        }

        viewModelScope.launch {
            authRepository.activeUsers.collect { users ->
                _uiState.update { it.copy(availableUsers = users) }
            }
        }

        viewModelScope.launch {
            combine(
                modbusClient.isConnected,
                modbusClient.temperatura1,
                modbusClient.temperatura2,
                modbusClient.humedad,
                modbusClient.tension,
                modbusClient.procesoActivo,
                modbusClient.zonas,
                modbusClient.estadoIrrigador,
                _uiState.map { it.currentUser?.idVehiculo ?: "AAA000" }.distinctUntilChanged(),
            ) { values ->
                val connected = values[0] as Boolean
                val t1 = values[1] as Double?
                val t2 = values[2] as Double?
                val hum = values[3] as Double?
                val ten = values[4] as Double?
                val proceso = values[5] as Boolean
                val zonas = values[6] as List<ZoneStatus>
                val irrigator = values[7] as ZoneStatus
                val vehiculo = values[8] as String
                
                val currentHistory = _uiState.value.hmiState.history
                val newHistory = if (proceso && t1 != null && t2 != null && hum != null) {
                    (currentHistory + com.pronavicola.camion.domain.model.HmiHistoryPoint(t1, t2, hum))
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
                    history = newHistory
                )
            }.collect { hmi ->
                _uiState.update { it.copy(hmiState = hmi) }
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
                
                if (user != null && hmi.procesoEstado == com.pronavicola.camion.domain.model.ProcesoEstado.ACTIVO) {
                    val registro = VariableRegistroEntity(
                        idVehiculo = user.idVehiculo ?: "AAA000",
                        fecha = dateFormat.format(Date()),
                        temperatura1 = hmi.temperatura1 ?: 0.0,
                        temperatura2 = hmi.temperatura2 ?: 0.0,
                        humedad = hmi.humedad ?: 0.0,
                    )
                    registrosRepository.saveRegistro(registro)
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
                            )
                        }
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

    private fun showMessage(message: String) {
        _uiState.update { it.copy(statusMessage = message) }
    }

    class Factory(private val app: CamionApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                app.authRepository,
                app.parametrosRepository,
                app.registrosRepository,
                app.modbusClient,
            ) as T
        }
    }
}

