package com.pronavicola.camion.presentation.config

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronavicola.camion.data.remote.FurgonInfo
import com.pronavicola.camion.data.remote.ScreensApiService
import com.pronavicola.camion.domain.repository.AppConfigRepository
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TruckAssignmentViewModel(
    private val screensApiService: ScreensApiService,
    private val appConfigRepository: AppConfigRepository,
) : ViewModel() {

    private val tag = "TruckAssignmentViewModel"

    private val _currentPlate = MutableStateFlow<String?>(null)
    val currentPlate: StateFlow<String?> = _currentPlate.asStateFlow()

    private val _availableFurgones = MutableStateFlow<List<FurgonInfo>>(emptyList())
    val availableFurgones: StateFlow<List<FurgonInfo>> = _availableFurgones.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadCurrentPlate()
    }

    fun loadCurrentPlate() {
        viewModelScope.launch {
            _currentPlate.value = appConfigRepository.getFurgonPlate()
        }
    }

    fun loadAvailableFurgones() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val baseUrl = appConfigRepository.getServerBaseUrl() ?: "https://screens-api.pronavicola.com"
                val deviceToken = appConfigRepository.getDeviceToken()

                val result = screensApiService.getAvailableFurgones(baseUrl, deviceToken)
                result.fold(
                    onSuccess = { response ->
                        _availableFurgones.value = response.availableFurgones
                        Log.d(tag, "Loaded ${response.availableFurgones.size} available furgones")
                    },
                    onFailure = { error ->
                        _errorMessage.value = error.message ?: "Error al cargar furgones disponibles"
                        Log.e(tag, "Failed to load available furgones: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error desconocido"
                Log.e(tag, "Exception loading available furgones: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun assignFurgon(furgon: FurgonInfo, onAssigned: (() -> Unit)? = null) {
        viewModelScope.launch {
            if (furgon.id.isBlank()) {
                _errorMessage.value = "ID de furgón inválido"
                return@launch
            }

            _isLoading.value = true
            _errorMessage.value = null

            try {
                val baseUrl = appConfigRepository.getServerBaseUrl() ?: "https://screens-api.pronavicola.com"
                val deviceToken = appConfigRepository.getDeviceToken()

                val result = screensApiService.updateAssociatedFurgon(baseUrl, deviceToken, furgon.id)
                result.fold(
                    onSuccess = { updatedFurgon ->
                        appConfigRepository.saveFurgonId(updatedFurgon.id)
                        appConfigRepository.saveFurgonPlate(updatedFurgon.plate)
                        _currentPlate.value = updatedFurgon.plate
                        Log.i(tag, "Furgon assigned successfully: ${updatedFurgon.plate}")
                        onAssigned?.invoke()
                    },
                    onFailure = { error ->
                        _errorMessage.value = error.message ?: "Error al asignar furgón"
                        Log.e(tag, "Failed to assign furgon: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error desconocido"
                Log.e(tag, "Exception assigning furgon: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@Composable
fun TruckAssignmentSection(
    currentPlate: String?,
    userProfileLevel: Int,
    onAssignPlate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Authorized: 0=Desarrollador, 1=Administrador, 2=Supervisor. Read-only for 3=Técnico, 4=Operario
    val canEdit = userProfileLevel in 0..2

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Placa Asignada",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = currentPlate ?: "Sin asignar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (canEdit) {
            IconButton(onClick = onAssignPlate) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar placa",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun TruckAssignmentDialog(
    availableFurgones: List<FurgonInfo>,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSelectFurgon: (FurgonInfo) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Furgón") },
        text = {
            Column {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                } else if (availableFurgones.isEmpty()) {
                    Text(
                        text = "No hay furgones disponibles para asignar.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    LazyColumn {
                        items(availableFurgones) { furgon ->
                            ListItem(
                                headlineContent = {
                                    Text(furgon.plate, fontWeight = FontWeight.Bold)
                                },
                                supportingContent = {
                                    val desc = "${furgon.brand ?: ""} ${furgon.model ?: ""}".trim()
                                    if (desc.isNotBlank()) Text(desc)
                                },
                                modifier = Modifier.clickable {
                                    onSelectFurgon(furgon)
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
