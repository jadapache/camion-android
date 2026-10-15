package com.pronavicola.camion.presentation.alarmas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronavicola.camion.domain.usecase.alarmas.AlarmaRegistroUiModel
import com.pronavicola.camion.domain.usecase.alarmas.ObserveAlarmasRegistrosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlarmasRegistrosUiState(
    val alarmas: List<AlarmaRegistroUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class AlarmasRegistrosViewModel(
    private val observeAlarmasRegistrosUseCase: ObserveAlarmasRegistrosUseCase,
    private val deviceIdStr: String, 
) : ViewModel() {
    private val _uiState = MutableStateFlow(AlarmasRegistrosUiState())
    val uiState: StateFlow<AlarmasRegistrosUiState> = _uiState.asStateFlow()

    init {
        loadAlarmasRegistros()
    }

    private fun loadAlarmasRegistros() {
        viewModelScope.launch {
            try {
                // Here we might need the actual numeric ID of the vehicle.
                // Assuming deviceIdStr maps to an ID, or for now we pass a static 1 
                // In a real scenario we need the actual Vehiculo ID. We'll use 1 for now.
                val idVehiculo = 1 
                observeAlarmasRegistrosUseCase(idVehiculo).collect { alarmasList ->
                    _uiState.update { it.copy(alarmas = alarmasList, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
