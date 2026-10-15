package com.pronavicola.camion.presentation.hmi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronavicola.camion.data.local.dao.VehiculoDao
import com.pronavicola.camion.data.remote.FurgonInfo
import com.pronavicola.camion.util.Log
import com.pronavicola.camion.domain.model.HmiHistoryPoint
import com.pronavicola.camion.domain.model.HmiState
import com.pronavicola.camion.domain.repository.AppConfigRepository
import com.pronavicola.camion.domain.repository.DevicePairingRepository
import com.pronavicola.camion.domain.usecase.config.UpdateTruckPlateUseCase
import com.pronavicola.camion.domain.usecase.hmi.ConnectModbusUseCase
import com.pronavicola.camion.domain.usecase.hmi.DisconnectModbusUseCase
import com.pronavicola.camion.domain.usecase.hmi.ObserveHmiStateUseCase
import com.pronavicola.camion.domain.usecase.hmi.StartProcessUseCase
import com.pronavicola.camion.domain.usecase.hmi.StopProcessUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class HmiUiState(
    val hmiState: HmiState = HmiState(),
    val deviceId: String = "ABC123",
    val furgonPlate: String = "AAA123",
    val historyPoints: List<HmiHistoryPoint> = emptyList(),
    val isConnecting: Boolean = false,
    val feedbackMessage: String? = null,
    val showTruckAssignmentDialog: Boolean = false,
    val availableFurgones: List<FurgonInfo> = emptyList(),
    val isLoadingFurgones: Boolean = false,
    val furgonAssignmentError: String? = null,
)

class HmiViewModel(
    private val observeHmiStateUseCase: ObserveHmiStateUseCase,
    private val startProcessUseCase: StartProcessUseCase,
    private val stopProcessUseCase: StopProcessUseCase,
    private val connectModbusUseCase: ConnectModbusUseCase,
    private val disconnectModbusUseCase: DisconnectModbusUseCase,
    private val appConfigRepository: AppConfigRepository,
    private val updateTruckPlateUseCase: UpdateTruckPlateUseCase? = null,
    private val devicePairingRepository: DevicePairingRepository? = null,
    private val vehiculoDao: VehiculoDao? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HmiUiState())
    val uiState: StateFlow<HmiUiState> = _uiState.asStateFlow()

    init {
        // Observe reactive vehicle table changes (e.g. from web app reassignment sync)
        if (vehiculoDao != null) {
            viewModelScope.launch {
                vehiculoDao.observePredeterminado().collect { vehiculo ->
                    if (vehiculo != null && vehiculo.placa.isNotBlank()) {
                        val devId = appConfigRepository.getDeviceId() ?: ""
                        if (!vehiculo.placa.equals(devId, ignoreCase = true)) {
                            _uiState.update { current ->
                                if (current.furgonPlate != vehiculo.placa) {
                                    Log.i("HmiViewModel", "Plate updated from DB: ${current.furgonPlate} -> ${vehiculo.placa}")
                                    current.copy(
                                        furgonPlate = vehiculo.placa,
                                        hmiState = current.hmiState.copy(idVehiculo = vehiculo.placa)
                                    )
                                } else {
                                    current
                                }
                            }
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            val devId = appConfigRepository.getDeviceId() ?: "ABC123"
            val rawPlate = appConfigRepository.getFurgonPlate() ?: "ABC123"
            val plate = if (rawPlate.isNotBlank() && rawPlate != devId) rawPlate else "ABC123"
            _uiState.update { it.copy(deviceId = devId, furgonPlate = plate) }

            observeHmiStateUseCase(plate).collect { rawState ->
                _uiState.update { current ->
                    val activePlate = if (current.furgonPlate.isNotBlank() && current.furgonPlate != current.deviceId) {
                        current.furgonPlate
                    } else {
                        "ABC123"
                    }
                    current.copy(
                        hmiState = rawState.copy(
                            idVehiculo = activePlate,
                            history = current.historyPoints
                        )
                    )
                }
            }
        }

        // Preload available furgones from API
        fetchAvailableFurgones()

        // Live Real-Time ECG Strip Chart sampling loop (continuous 500ms scrolling)
        viewModelScope.launch {
            while (true) {
                delay(500L)
                val current = _uiState.value
                val raw = current.hmiState
                if (raw.conectado && raw.temperatura1 != null && raw.temperatura2 != null && raw.humedad != null) {
                    val point = HmiHistoryPoint(
                        t1 = raw.temperatura1,
                        t2 = raw.temperatura2,
                        hum = raw.humedad,
                        timestamp = System.currentTimeMillis()
                    )
                    _uiState.update { state ->
                        val updatedList = (state.historyPoints + point).takeLast(50)
                        state.copy(
                            historyPoints = updatedList,
                            hmiState = state.hmiState.copy(history = updatedList)
                        )
                    }
                }
            }
        }
    }

    fun startProcess() {
        viewModelScope.launch {
            try {
                startProcessUseCase()
                _uiState.update { it.copy(feedbackMessage = "Comando INICIAR PROCESO enviado al PLC") }
            } catch (e: Exception) {
                _uiState.update { it.copy(feedbackMessage = "Error al iniciar proceso: ${e.message}") }
            }
        }
    }

    fun stopProcess() {
        viewModelScope.launch {
            try {
                stopProcessUseCase()
                _uiState.update { it.copy(feedbackMessage = "Comando DETENER PROCESO enviado al PLC") }
            } catch (e: Exception) {
                _uiState.update { it.copy(feedbackMessage = "Error al detener proceso: ${e.message}") }
            }
        }
    }

    fun reconnectModbus() {
        viewModelScope.launch {
            val host = appConfigRepository.getModbusHost() ?: "127.0.0.1"
            val port = appConfigRepository.getModbusPort()?.toIntOrNull() ?: 502
            connectModbusUseCase(host, port)
        }
    }

    fun updatePlate(newPlate: String) {
        val plate = newPlate.trim().uppercase()
        if (plate.isNotBlank()) {
            _uiState.update { current ->
                current.copy(
                    furgonPlate = plate,
                    hmiState = current.hmiState.copy(idVehiculo = plate)
                )
            }
        }
    }

    fun reloadPlate() {
        viewModelScope.launch {
            val devId = appConfigRepository.getDeviceId() ?: ""
            val rawPlate = appConfigRepository.getFurgonPlate() ?: "ABC123"
            val effective = if (rawPlate.isNotBlank() && rawPlate != devId) rawPlate else "ABC123"
            _uiState.update { current ->
                current.copy(
                    deviceId = devId,
                    furgonPlate = effective,
                    hmiState = current.hmiState.copy(idVehiculo = effective)
                )
            }
        }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun openTruckAssignmentDialog() {
        _uiState.update { it.copy(showTruckAssignmentDialog = true, furgonAssignmentError = null) }
        fetchAvailableFurgones()
    }

    fun closeTruckAssignmentDialog() {
        _uiState.update { it.copy(showTruckAssignmentDialog = false, furgonAssignmentError = null) }
    }

    fun fetchAvailableFurgones() {
        val repo = devicePairingRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFurgones = true, furgonAssignmentError = null) }
            try {
                val serverUrl = appConfigRepository.getServerBaseUrl() ?: ""
                val token = appConfigRepository.getDeviceToken()
                val devId = appConfigRepository.getDeviceId()
                val result = repo.getAvailableFurgones(serverUrl, token, devId)
                if (result.isSuccess) {
                    _uiState.update { it.copy(isLoadingFurgones = false, availableFurgones = result.getOrThrow()) }
                } else {
                    _uiState.update { it.copy(isLoadingFurgones = false) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingFurgones = false) }
            }
        }
    }

    fun assignTruckPlate(newPlate: String) {
        val plate = newPlate.trim().uppercase()
        if (plate.isBlank()) {
            _uiState.update { it.copy(furgonAssignmentError = "La placa no puede estar vacía") }
            return
        }
        val currentDevId = _uiState.value.deviceId
        if (currentDevId.isNotBlank() && plate == currentDevId) {
            _uiState.update { it.copy(furgonAssignmentError = "No se puede asignar el Device ID como placa") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFurgones = true, furgonAssignmentError = null) }

            // 1. Notificar a la API de FURGN-BI si está configurada
            val repo = devicePairingRepository
            if (repo != null) {
                try {
                    val serverUrl = appConfigRepository.getServerBaseUrl() ?: ""
                    val token = appConfigRepository.getDeviceToken()
                    val targetFurgon = _uiState.value.availableFurgones.find { it.plate.equals(plate, ignoreCase = true) }
                    val furgonId = targetFurgon?.id ?: plate
                    repo.updateAssociatedFurgon(serverUrl, token, furgonId, plate)
                } catch (e: Exception) {
                    Log.w("HmiViewModel", "No se pudo sincronizar placa con backend: ${e.message}")
                }
            }

            // 2. Persistir localmente en SQLite
            val useCase = updateTruckPlateUseCase
            if (useCase != null) {
                val result = useCase(plate)
                if (result.isSuccess) {
                    _uiState.update { current ->
                        current.copy(
                            furgonPlate = plate,
                            hmiState = current.hmiState.copy(idVehiculo = plate),
                            showTruckAssignmentDialog = false,
                            isLoadingFurgones = false,
                            feedbackMessage = "Furgón $plate asignado correctamente",
                        )
                    }
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Error al asignar furgón"
                    _uiState.update { it.copy(isLoadingFurgones = false, furgonAssignmentError = err) }
                }
            } else {
                appConfigRepository.saveFurgonPlate(plate)
                _uiState.update { current ->
                    current.copy(
                        furgonPlate = plate,
                        hmiState = current.hmiState.copy(idVehiculo = plate),
                        showTruckAssignmentDialog = false,
                        isLoadingFurgones = false,
                        feedbackMessage = "Furgón $plate asignado correctamente",
                    )
                }
            }
        }
    }
}
