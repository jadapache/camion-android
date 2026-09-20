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
    val baseT1Input: String = "22.0",
    val baseT2Input: String = "22.5",
    val baseHumInput: String = "65.0",
    val baseVoltInput: String = "24.0",
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

    fun onBaseT1Change(value: String) = _uiState.update { it.copy(baseT1Input = value) }
    fun onBaseT2Change(value: String) = _uiState.update { it.copy(baseT2Input = value) }
    fun onBaseHumChange(value: String) = _uiState.update { it.copy(baseHumInput = value) }
    fun onBaseVoltChange(value: String) = _uiState.update { it.copy(baseVoltInput = value) }

    fun applyParameters() {
        val t1 = _uiState.value.baseT1Input.toDoubleOrNull() ?: 22.0
        val t2 = _uiState.value.baseT2Input.toDoubleOrNull() ?: 22.5
        val hum = _uiState.value.baseHumInput.toDoubleOrNull() ?: 65.0
        val volt = _uiState.value.baseVoltInput.toDoubleOrNull() ?: 24.0

        val newConfig = _uiState.value.simulationState.config.copy(
            baselineTemperature1 = t1,
            baselineTemperature2 = t2,
            baselineHumidity = hum,
            baselineVoltage = volt,
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
        val t1 = _uiState.value.baseT1Input.toDoubleOrNull() ?: 22.0
        val t2 = _uiState.value.baseT2Input.toDoubleOrNull() ?: 22.5
        val hum = _uiState.value.baseHumInput.toDoubleOrNull() ?: 65.0
        val volt = _uiState.value.baseVoltInput.toDoubleOrNull() ?: 24.0
        return SimulationConfig(
            baselineTemperature1 = t1,
            baselineTemperature2 = t2,
            baselineHumidity = hum,
            baselineVoltage = volt,
            gateOpen = _uiState.value.isGateOpen,
        )
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
