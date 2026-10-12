package com.pronavicola.camion.presentation.registros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronavicola.camion.data.local.dao.VariableRegistroDao
import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.domain.repository.AppConfigRepository
import com.pronavicola.camion.domain.usecase.registros.BuildTelemetryPayloadUseCase
import com.pronavicola.camion.domain.usecase.registros.ClearAllRegistrosUseCase
import com.pronavicola.camion.domain.usecase.registros.ExportRegistrosCsvUseCase
import com.pronavicola.camion.domain.usecase.registros.ObserveRegistrosUseCase
import com.pronavicola.camion.domain.usecase.registros.ResendRegistroUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class RegistrosUiState(
    val registros: List<VariableRegistroEntity> = emptyList(),
    val filterFechaDesde: String = "",
    val filterFechaHasta: String = "",
    val filterPlaca: String = "",
    val filterEstado: String = "TODOS", // TODOS, ENVIADO, PENDIENTE
    val sortColumn: SortColumn = SortColumn.NONE,
    val sortDirection: SortDirection = SortDirection.NONE,
    val selectedRegistroForPayload: VariableRegistroEntity? = null,
    val payloadJsonPreview: String? = null,
    val statusMessage: String? = null,
    val isSending: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val deviceId: String = "ABC123",
) {
    val filteredRegistros: List<VariableRegistroEntity>
        get() {
            val desdeMillis = if (filterFechaDesde.isNotBlank()) dateStringToMillis(filterFechaDesde) else null
            val hastaMillis = if (filterFechaHasta.isNotBlank()) dateStringToMillis(filterFechaHasta)?.plus(86400000L - 1) else null

            var result = registros.filter { r ->
                val rMillis = parseRecordDateToMillis(r.fecha)
                val matchesFecha = when {
                    desdeMillis != null && hastaMillis != null -> rMillis in desdeMillis..hastaMillis
                    desdeMillis != null -> rMillis >= desdeMillis
                    hastaMillis != null -> rMillis <= hastaMillis
                    else -> true
                }
                val matchesPlaca = filterPlaca.isBlank() || r.idVehiculo.toString().contains(filterPlaca, ignoreCase = true)
                val matchesEstado = when (filterEstado) {
                    "ENVIADO" -> r.estadoEnvio.equals("Enviado", ignoreCase = true)
                    "PENDIENTE" -> !r.estadoEnvio.equals("Enviado", ignoreCase = true)
                    else -> true
                }
                matchesFecha && matchesPlaca && matchesEstado
            }

            if (sortColumn != SortColumn.NONE && sortDirection != SortDirection.NONE) {
                result = when (sortColumn) {
                    SortColumn.FECHA -> if (sortDirection == SortDirection.ASC) result.sortedBy { it.fecha } else result.sortedByDescending { it.fecha }
                    SortColumn.T1 -> if (sortDirection == SortDirection.ASC) result.sortedBy { it.temperatura1 } else result.sortedByDescending { it.temperatura1 }
                    SortColumn.T2 -> if (sortDirection == SortDirection.ASC) result.sortedBy { it.temperatura2 } else result.sortedByDescending { it.temperatura2 }
                    SortColumn.HUM -> if (sortDirection == SortDirection.ASC) result.sortedBy { it.humedad } else result.sortedByDescending { it.humedad }
                    SortColumn.TENSION -> if (sortDirection == SortDirection.ASC) result.sortedBy { it.tension } else result.sortedByDescending { it.tension }
                    SortColumn.ESTADO -> if (sortDirection == SortDirection.ASC) result.sortedBy { it.estadoEnvio } else result.sortedByDescending { it.estadoEnvio }
                    else -> result
                }
            }
            return result
        }

    private fun dateStringToMillis(dateStr: String): Long? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(dateStr)?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun parseRecordDateToMillis(fechaStr: String): Long {
        return try {
            val formats = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd")
            for (pattern in formats) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                    val parsed = sdf.parse(fechaStr)
                    if (parsed != null) return parsed.time
                } catch (_: Exception) {}
            }
            0L
        } catch (_: Exception) {
            0L
        }
    }
}

enum class SortColumn {
    FECHA, T1, T2, HUM, TENSION, ESTADO, NONE
}

enum class SortDirection {
    ASC, DESC, NONE
}

class RegistrosViewModel(
    private val observeRegistrosUseCase: ObserveRegistrosUseCase,
    private val resendRegistroUseCase: ResendRegistroUseCase,
    private val clearAllRegistrosUseCase: ClearAllRegistrosUseCase,
    private val exportRegistrosCsvUseCase: ExportRegistrosCsvUseCase,
    private val buildTelemetryPayloadUseCase: BuildTelemetryPayloadUseCase,
    private val appConfigRepository: AppConfigRepository,
    private val variableRegistroDao: VariableRegistroDao? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrosUiState())
    val uiState: StateFlow<RegistrosUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private val pageSize = 50
    private var isLastPageReached = false

    init {
        viewModelScope.launch {
            val devId = appConfigRepository.getDeviceId() ?: "ABC123"
            _uiState.update { it.copy(deviceId = devId) }
        }
    }

    fun loadRegistros(user: SessionUser) {
        viewModelScope.launch {
            observeRegistrosUseCase(user).collect { list ->
                _uiState.update { it.copy(registros = list) }
            }
        }
    }

    fun loadNextPage() {
        val dao = variableRegistroDao ?: return
        if (_uiState.value.isLoadingMore || isLastPageReached) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val nextPage = currentPage + 1
            val items = dao.getPagedRecords(
                limit = pageSize,
                offset = nextPage * pageSize,
                startDate = _uiState.value.filterFechaDesde.ifBlank { null },
                endDate = _uiState.value.filterFechaHasta.ifBlank { null },
                plate = _uiState.value.filterPlaca.ifBlank { null },
                status = if (_uiState.value.filterEstado == "TODOS") null else _uiState.value.filterEstado
            )
            if (items.isEmpty()) {
                isLastPageReached = true
            } else {
                currentPage = nextPage
                if (items.size < pageSize) {
                    isLastPageReached = true
                }
            }
            _uiState.update { current ->
                val combined = (current.registros + items).distinctBy { it.idRegistro }
                current.copy(
                    registros = combined,
                    isLoadingMore = false,
                    canLoadMore = !isLastPageReached
                )
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

    fun clearFilters() {
        _uiState.update {
            it.copy(
                filterFechaDesde = "",
                filterFechaHasta = "",
                filterPlaca = "",
                filterEstado = "TODOS",
            )
        }
    }

    fun toggleSort(column: SortColumn) {
        _uiState.update { current ->
            if (current.sortColumn == column) {
                val nextDirection = when (current.sortDirection) {
                    SortDirection.NONE -> SortDirection.ASC
                    SortDirection.ASC -> SortDirection.DESC
                    SortDirection.DESC -> SortDirection.NONE
                }
                current.copy(
                    sortDirection = nextDirection,
                    sortColumn = if (nextDirection == SortDirection.NONE) SortColumn.NONE else column,
                )
            } else {
                current.copy(
                    sortColumn = column,
                    sortDirection = SortDirection.ASC,
                )
            }
        }
    }

    fun openPayloadPreview(registro: VariableRegistroEntity) {
        val payload = buildTelemetryPayloadUseCase(registro, _uiState.value.deviceId)
        _uiState.update {
            it.copy(
                selectedRegistroForPayload = registro,
                payloadJsonPreview = payload,
            )
        }
    }

    fun closePayloadPreview() {
        _uiState.update {
            it.copy(
                selectedRegistroForPayload = null,
                payloadJsonPreview = null,
            )
        }
    }

    fun resendRegistro(registro: VariableRegistroEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            val success = resendRegistroUseCase(registro, _uiState.value.deviceId)
            _uiState.update {
                it.copy(
                    isSending = false,
                    selectedRegistroForPayload = null,
                    payloadJsonPreview = null,
                    statusMessage = if (success) "Registro #${registro.idRegistro} re-enviado con éxito" else "Registro #${registro.idRegistro} encolado en Outbox (sin conexión directa)",
                )
            }
        }
    }

    fun clearAllRegistros(user: SessionUser) {
        viewModelScope.launch {
            val cleared = clearAllRegistrosUseCase(user)
            _uiState.update {
                it.copy(
                    statusMessage = if (cleared) "Histórico de registros eliminado exitosamente" else "No tiene permisos para eliminar registros",
                )
            }
        }
    }

    fun exportCsv(): String {
        val csv = exportRegistrosCsvUseCase(_uiState.value.filteredRegistros)
        _uiState.update { it.copy(statusMessage = "CSV con ${_uiState.value.filteredRegistros.size} registros generado") }
        return csv
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
