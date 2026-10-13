package com.pronavicola.camion.presentation.alarmas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.domain.usecase.alarmas.AlarmaRegistroUiModel
import com.pronavicola.camion.domain.usecase.alarmas.ClearAllAlarmasRegistrosUseCase
import com.pronavicola.camion.domain.usecase.alarmas.ExportAlarmasCsvUseCase
import com.pronavicola.camion.domain.usecase.alarmas.ObserveAlarmasRegistrosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlarmasRegistrosUiState(
    val alarmas: List<AlarmaRegistroUiModel> = emptyList(),
    val filterFechaDesde: String = "",
    val filterFechaHasta: String = "",
    val filterEstado: String = "TODOS", // TODOS, ACTIVA, INACTIVA
    val isLoading: Boolean = true,
    val error: String? = null,
    val statusMessage: String? = null,
    val isClearing: Boolean = false,
) {
    val filteredAlarmas: List<AlarmaRegistroUiModel>
        get() {
            return alarmas.filter { a ->
                val datePart = a.horaInicio.substringBefore(" ").trim()
                val matchDesde = filterFechaDesde.isBlank() || datePart >= filterFechaDesde.trim()
                val matchHasta = filterFechaHasta.isBlank() || datePart <= filterFechaHasta.trim()
                val matchEstado = when (filterEstado.uppercase()) {
                    "ACTIVA" -> a.estado.equals("Activa", ignoreCase = true)
                    "INACTIVA" -> !a.estado.equals("Activa", ignoreCase = true)
                    else -> true
                }
                matchDesde && matchHasta && matchEstado
            }
        }
}

class AlarmasRegistrosViewModel(
    private val observeAlarmasRegistrosUseCase: ObserveAlarmasRegistrosUseCase,
    private val clearAllAlarmasRegistrosUseCase: ClearAllAlarmasRegistrosUseCase,
    private val exportAlarmasCsvUseCase: ExportAlarmasCsvUseCase,
    private val deviceIdStr: String = "",
) : ViewModel() {
    private val _uiState = MutableStateFlow(AlarmasRegistrosUiState())
    val uiState: StateFlow<AlarmasRegistrosUiState> = _uiState.asStateFlow()

    init {
        loadAlarmasRegistros()
    }

    private fun loadAlarmasRegistros() {
        viewModelScope.launch {
            try {
                // Observe all alarms records (or for current vehicle if specified)
                observeAlarmasRegistrosUseCase().collect { alarmasList ->
                    _uiState.update { it.copy(alarmas = alarmasList, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun setFilterFechaDesde(date: String) {
        _uiState.update { it.copy(filterFechaDesde = date) }
    }

    fun setFilterFechaHasta(date: String) {
        _uiState.update { it.copy(filterFechaHasta = date) }
    }

    fun setFilterEstado(estado: String) {
        _uiState.update { it.copy(filterEstado = estado) }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                filterFechaDesde = "",
                filterFechaHasta = "",
                filterEstado = "TODOS"
            )
        }
    }

    fun clearAllAlarmasRegistros(user: SessionUser) {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearing = true) }
            val cleared = clearAllAlarmasRegistrosUseCase(user)
            _uiState.update {
                it.copy(
                    isClearing = false,
                    statusMessage = if (cleared) "Histórico de alarmas eliminado exitosamente" else "No tiene permisos para eliminar registros de alarmas",
                )
            }
        }
    }

    fun exportCsv(): String {
        val csv = exportAlarmasCsvUseCase(_uiState.value.filteredAlarmas)
        _uiState.update { it.copy(statusMessage = "CSV con ${_uiState.value.filteredAlarmas.size} alarmas generado") }
        return csv
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
