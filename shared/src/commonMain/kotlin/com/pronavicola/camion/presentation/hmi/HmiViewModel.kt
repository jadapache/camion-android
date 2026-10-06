package com.pronavicola.camion.presentation.hmi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronavicola.camion.domain.model.HmiHistoryPoint
import com.pronavicola.camion.domain.model.HmiState
import com.pronavicola.camion.domain.repository.AppConfigRepository
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
    val furgonPlate: String = "AAA000",
    val historyPoints: List<HmiHistoryPoint> = emptyList(),
    val isConnecting: Boolean = false,
    val feedbackMessage: String? = null,
)

class HmiViewModel(
    private val observeHmiStateUseCase: ObserveHmiStateUseCase,
    private val startProcessUseCase: StartProcessUseCase,
    private val stopProcessUseCase: StopProcessUseCase,
    private val connectModbusUseCase: ConnectModbusUseCase,
    private val disconnectModbusUseCase: DisconnectModbusUseCase,
    private val appConfigRepository: AppConfigRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HmiUiState())
    val uiState: StateFlow<HmiUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val devId = appConfigRepository.getDeviceId() ?: "ABC123"
            val plate = appConfigRepository.getFurgonPlate() ?: "AAA000"
            _uiState.update { it.copy(deviceId = devId, furgonPlate = plate) }

            observeHmiStateUseCase(devId).collect { rawState ->
                _uiState.update { current ->
                    current.copy(
                        hmiState = rawState.copy(
                            idVehiculo = plate.ifBlank { devId },
                            history = current.historyPoints
                        )
                    )
                }
            }
        }

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

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
