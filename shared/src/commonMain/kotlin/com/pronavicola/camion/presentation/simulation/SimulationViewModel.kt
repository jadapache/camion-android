package com.pronavicola.camion.presentation.simulation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronavicola.camion.domain.model.SimulationConfig
import com.pronavicola.camion.domain.model.SimulationState
import com.pronavicola.camion.domain.usecase.simulation.GetSimulationStateUseCase
import com.pronavicola.camion.domain.usecase.simulation.InjectSimulatedFaultUseCase
import com.pronavicola.camion.domain.usecase.simulation.ToggleSimulationUseCase
import com.pronavicola.camion.domain.usecase.simulation.UpdateSimulationParamsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SimulationUiState(
    val simulationState: SimulationState = SimulationState(),
    val baseT1: Float = 22.0f,
    val baseT2: Float = 22.5f,
    val baseHum: Float = 65.0f,
    val baseVolt: Float = 24.0f,
    val isGateOpen: Boolean = false,
    val activeAlarmBits: Set<Int> = emptySet(),
    val message: String? = null,
)

class SimulationViewModel(
    private val getSimulationStateUseCase: GetSimulationStateUseCase,
    private val toggleSimulationUseCase: ToggleSimulationUseCase,
    private val updateSimulationParamsUseCase: UpdateSimulationParamsUseCase,
    private val injectSimulatedFaultUseCase: InjectSimulatedFaultUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SimulationUiState())
    val uiState: StateFlow<SimulationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getSimulationStateUseCase().collect { simState ->
                _uiState.update {
                    it.copy(
                        simulationState = simState,
                        baseT1 = simState.config.baselineTemperature1.toFloat(),
                        baseT2 = simState.config.baselineTemperature2.toFloat(),
                        baseHum = simState.config.baselineHumidity.toFloat(),
                        baseVolt = simState.config.baselineVoltage.toFloat(),
                        isGateOpen = simState.config.gateOpen,
                    )
                }
            }
        }
    }

    fun toggleServer(enable: Boolean) {
        val cfg = getCurrentConfig()
        toggleSimulationUseCase(enable, cfg)
        _uiState.update {
            it.copy(
                message = if (enable) "Servidor simulado iniciado en puerto ${cfg.port}" else "Servidor simulado detenido"
            )
        }
    }

    fun onBaseT1Change(value: Float) = _uiState.update { it.copy(baseT1 = value) }
    fun onBaseT2Change(value: Float) = _uiState.update { it.copy(baseT2 = value) }
    fun onBaseHumChange(value: Float) = _uiState.update { it.copy(baseHum = value) }
    fun onBaseVoltChange(value: Float) = _uiState.update { it.copy(baseVolt = value) }

    fun applyParameters() {
        val stateVal = _uiState.value
        val newConfig = stateVal.simulationState.config.copy(
            baselineTemperature1 = stateVal.baseT1.toDouble(),
            baselineTemperature2 = stateVal.baseT2.toDouble(),
            baselineHumidity = stateVal.baseHum.toDouble(),
            baselineVoltage = stateVal.baseVolt.toDouble(),
        )
        updateSimulationParamsUseCase(newConfig)
        _uiState.update { it.copy(message = "Parámetros de simulación aplicados en caliente") }
    }

    fun toggleGate(open: Boolean) {
        updateSimulationParamsUseCase.setGateOpen(open)
        _uiState.update { it.copy(isGateOpen = open) }
    }

    fun toggleAlarmBit(bit: Int) {
        val currentBits = _uiState.value.activeAlarmBits
        val nextActive = !currentBits.contains(bit)
        val newBits = if (nextActive) currentBits + bit else currentBits - bit

        _uiState.update { it.copy(activeAlarmBits = newBits) }
        injectSimulatedFaultUseCase(bit, nextActive)
    }

    private fun getCurrentConfig(): SimulationConfig {
        val stateVal = _uiState.value
        return SimulationConfig(
            baselineTemperature1 = stateVal.baseT1.toDouble(),
            baselineTemperature2 = stateVal.baseT2.toDouble(),
            baselineHumidity = stateVal.baseHum.toDouble(),
            baselineVoltage = stateVal.baseVolt.toDouble(),
            gateOpen = stateVal.isGateOpen,
        )
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
