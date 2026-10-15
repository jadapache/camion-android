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
import com.pronavicola.camion.domain.usecase.registros.GetRegistrosPagedUseCase
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
import kotlin.math.ceil

data class RegistrosUiState(
    val registros: List<VariableRegistroEntity> = emptyList(),
    val filterFechaDesde: String = "",
    val filterFechaHasta: String = "",
    val filterPlaca: String = "",
    val sortColumn: SortColumn = SortColumn.NONE,
    val sortDirection: SortDirection = SortDirection.NONE,
    val selectedRegistroForPayload: VariableRegistroEntity? = null,
    val payloadJsonPreview: String? = null,
    val statusMessage: String? = null,
    val isSending: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val deviceId: String = "ABC123",
    val currentPage: Int = 1,
    val pageSize: Int = 25,
    val totalCount: Int = 0,
    val isLoadingPage: Boolean = false,
) {
    val totalPages: Int
        get() = maxOf(1, ceil(filteredRegistros.size.toDouble() / pageSize).toInt())

    val paginatedRegistros: List<VariableRegistroEntity>
        get() {
            val offset = (currentPage - 1) * pageSize
            return filteredRegistros.drop(offset).take(pageSize)
        }

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
                val matchesPlaca = filterPlaca.isBlank() || 
                    r.placa.contains(filterPlaca.trim(), ignoreCase = true) || 
                    r.idVehiculo.toString().contains(filterPlaca.trim(), ignoreCase = true)
                matchesFecha && matchesPlaca
            }

            if (sortColumn != SortColumn.NONE && sortDirection != SortDirection.NONE) {
                result = when (sortColumn) {
                    SortColumn.PLACA -> if (sortDirection == SortDirection.ASC) result.sortedBy { it.placa } else result.sortedByDescending { it.placa }
                    SortColumn.FECHA -> if (sortDirection == SortDirection.ASC) result.sortedBy { it.fecha } else result.sortedByDescending { it.fecha }
                    SortColumn.T1 -> if (sortDirection == SortDirection.ASC) result.sortedBy { it.temperatura1 } else result.sortedByDescending { it.temperatura1 }
                    SortColumn.T2 -> if (sortDirection == SortDirection.ASC) result.sortedBy { it.temperatura2 } else result.sortedByDescending { it.temperatura2 }
                    SortColumn.HUM -> if (sortDirection == SortDirection.ASC) result.sortedBy { it.humedad } else result.sortedByDescending { it.humedad }
                    SortColumn.TENSION -> if (sortDirection == SortDirection.ASC) result.sortedBy { it.tension } else result.sortedByDescending { it.tension }
                    SortColumn.NONE -> result
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
    PLACA, FECHA, T1, T2, HUM, TENSION, NONE
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
    private val getRegistrosPagedUseCase: GetRegistrosPagedUseCase? = null,
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

    fun setPage(page: Int) {
        _uiState.update { current ->
            val maxPage = current.totalPages
            val clamped = page.coerceIn(1, maxPage)
            current.copy(currentPage = clamped)
        }
    }

    fun setPageSize(size: Int) {
        _uiState.update { current ->
            current.copy(pageSize = size, currentPage = 1)
        }
    }

    fun nextPage() {
        setPage(_uiState.value.currentPage + 1)
    }

    fun prevPage() {
        setPage(_uiState.value.currentPage - 1)
    }

    fun loadRegistrosPaged(page: Int, pageSize: Int = 25, idVehiculo: Int? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPage = true, currentPage = page, pageSize = pageSize) }
            val offset = (page - 1) * pageSize
            val paged = getRegistrosPagedUseCase?.invoke(idVehiculo, pageSize, offset) ?: emptyList()
            val total = getRegistrosPagedUseCase?.getCount(idVehiculo) ?: paged.size
            _uiState.update {
                it.copy(
                    registros = paged,
                    totalCount = total,
                    isLoadingPage = false,
                )
            }
        }
    }

    fun setFilterFechaDesde(date: String) {
        _uiState.update { it.copy(filterFechaDesde = date, currentPage = 1) }
    }

    fun setFilterFechaHasta(date: String) {
        _uiState.update { it.copy(filterFechaHasta = date, currentPage = 1) }
    }

    fun setFilterPlaca(placa: String) {
        _uiState.update { it.copy(filterPlaca = placa, currentPage = 1) }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                filterFechaDesde = "",
                filterFechaHasta = "",
                filterPlaca = "",
                currentPage = 1,
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
                    currentPage = 1,
                )
            } else {
                current.copy(
                    sortColumn = column,
                    sortDirection = SortDirection.ASC,
                    currentPage = 1,
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
