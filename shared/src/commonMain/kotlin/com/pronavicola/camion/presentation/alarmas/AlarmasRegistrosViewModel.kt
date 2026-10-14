package com.pronavicola.camion.presentation.alarmas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.domain.usecase.alarmas.AlarmaRegistroUiModel
import com.pronavicola.camion.domain.usecase.alarmas.ClearAllAlarmasRegistrosUseCase
import com.pronavicola.camion.domain.usecase.alarmas.ExportAlarmasCsvUseCase
import com.pronavicola.camion.domain.usecase.alarmas.ObserveAlarmasRegistrosUseCase
import com.pronavicola.camion.presentation.registros.SortDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AlarmaSortColumn {
    NONE, PLACA, ALARMA, INICIO, FIN, ESTADO
}

data class AlarmasRegistrosUiState(
    val alarmas: List<AlarmaRegistroUiModel> = emptyList(),
    val filterFechaDesde: String = "",
    val filterFechaHasta: String = "",
    val filterPlaca: String = "",
    val filterEstado: String = "TODOS", // TODOS, ACTIVA, INACTIVA
    val sortColumn: AlarmaSortColumn = AlarmaSortColumn.NONE,
    val sortDirection: SortDirection = SortDirection.NONE,
    val isLoading: Boolean = true,
    val error: String? = null,
    val statusMessage: String? = null,
    val isClearing: Boolean = false,
) {
    val filteredAlarmas: List<AlarmaRegistroUiModel>
        get() {
            val trimmedPlaca = filterPlaca.trim()
            var result = alarmas.filter { a ->
                val datePart = a.horaInicio.substringBefore(" ").trim()
                val matchDesde = filterFechaDesde.isBlank() || datePart >= filterFechaDesde.trim()
                val matchHasta = filterFechaHasta.isBlank() || datePart <= filterFechaHasta.trim()
                val matchPlaca = trimmedPlaca.isBlank() ||
                    a.placa.contains(trimmedPlaca, ignoreCase = true) ||
                    a.idVehiculo.toString().contains(trimmedPlaca, ignoreCase = true)
                val matchEstado = when (filterEstado.uppercase()) {
                    "ACTIVA" -> a.estado.equals("Activa", ignoreCase = true)
                    "INACTIVA" -> !a.estado.equals("Activa", ignoreCase = true)
                    else -> true
                }
                matchDesde && matchHasta && matchPlaca && matchEstado
            }

            if (sortColumn != AlarmaSortColumn.NONE && sortDirection != SortDirection.NONE) {
                result = when (sortColumn) {
                    AlarmaSortColumn.PLACA -> if (sortDirection == SortDirection.ASC) result.sortedBy { it.placa } else result.sortedByDescending { it.placa }
                    AlarmaSortColumn.ALARMA -> if (sortDirection == SortDirection.ASC) result.sortedBy { it.alarmaDescripcion } else result.sortedByDescending { it.alarmaDescripcion }
                    AlarmaSortColumn.INICIO -> if (sortDirection == SortDirection.ASC) result.sortedBy { it.horaInicio } else result.sortedByDescending { it.horaInicio }
                    AlarmaSortColumn.FIN -> if (sortDirection == SortDirection.ASC) result.sortedBy { it.horaFin } else result.sortedByDescending { it.horaFin }
                    AlarmaSortColumn.ESTADO -> if (sortDirection == SortDirection.ASC) result.sortedBy { it.estado ?: "" } else result.sortedByDescending { it.estado ?: "" }
                    AlarmaSortColumn.NONE -> result
                }
            }
            return result
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

    fun setFilterPlaca(placa: String) {
        _uiState.update { it.copy(filterPlaca = placa) }
    }

    fun setFilterEstado(estado: String) {
        _uiState.update { it.copy(filterEstado = estado) }
    }

    fun toggleSort(column: AlarmaSortColumn) {
        _uiState.update { current ->
            if (current.sortColumn == column) {
                val newDir = when (current.sortDirection) {
                    SortDirection.NONE -> SortDirection.ASC
                    SortDirection.ASC -> SortDirection.DESC
                    SortDirection.DESC -> SortDirection.NONE
                }
                current.copy(
                    sortDirection = newDir,
                    sortColumn = if (newDir == SortDirection.NONE) AlarmaSortColumn.NONE else column
                )
            } else {
                current.copy(sortColumn = column, sortDirection = SortDirection.ASC)
            }
        }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                filterFechaDesde = "",
                filterFechaHasta = "",
                filterPlaca = "",
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
