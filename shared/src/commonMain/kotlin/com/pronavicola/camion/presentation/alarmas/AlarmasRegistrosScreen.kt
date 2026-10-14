package com.pronavicola.camion.presentation.alarmas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.domain.usecase.alarmas.AlarmaRegistroUiModel
import com.pronavicola.camion.presentation.registros.SortDirection
import com.pronavicola.camion.presentation.theme.CamionColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmasRegistrosScreen(
    viewModel: AlarmasRegistrosViewModel,
    currentUser: SessionUser? = null,
    onExportCsv: (fileName: String, csvContent: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }

    // Filtering State
    var showDatePickerFor by remember { mutableStateOf<String?>(null) } // "DESDE", "HASTA" or null
    var showFiltersPanel by remember { mutableStateOf(false) }

    // Pagination State
    var currentPage by remember { mutableStateOf(1) }
    var pageSize by remember { mutableStateOf(25) }
    val pageSizeOptions = remember { listOf(15, 25, 50, 100) }

    // Roles permitted to clean records: Desarrollador (0), Administrador (1), Supervisor (2)
    val canClearRecords = currentUser != null && currentUser.idPerfil in listOf(0, 1, 2)

    val displayedAlarmas = uiState.filteredAlarmas
    val totalRecords = displayedAlarmas.size
    val totalPages = remember(totalRecords, pageSize) {
        maxOf(1, ceil(totalRecords.toDouble() / pageSize).toInt())
    }

    // Reset page to 1 whenever filters or pageSize change
    LaunchedEffect(uiState.filterFechaDesde, uiState.filterFechaHasta, uiState.filterEstado, pageSize) {
        currentPage = 1
    }

    LaunchedEffect(totalPages) {
        if (currentPage > totalPages) {
            currentPage = totalPages
        }
    }

    val validCurrentPage = currentPage.coerceIn(1, totalPages)

    // Paginated slice for current page
    val paginatedAlarmas = remember(displayedAlarmas, validCurrentPage, pageSize) {
        val offset = (validCurrentPage - 1) * pageSize
        displayedAlarmas.drop(offset).take(pageSize)
    }

    val fromRecord = if (totalRecords == 0) 0 else ((validCurrentPage - 1) * pageSize) + 1
    val toRecord = minOf(validCurrentPage * pageSize, totalRecords)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CamionColors.panelBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- HEADER SECTION ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "HISTÓRICO DE ALARMAS",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "$totalRecords de ${uiState.alarmas.size} alarmas encontradas • Pág $validCurrentPage de $totalPages",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                )
            }

            // Top Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Toggle Filter Panel Button
                Button(
                    onClick = { showFiltersPanel = !showFiltersPanel },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showFiltersPanel) CamionColors.headerBackground.copy(alpha = 0.8f) else CamionColors.headerBackground
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Filtros", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }

                // Export CSV Button (Genera el archivo temporal y abre el selector nativo de Android)
                Button(
                    onClick = {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        val fileName = "alarmas_registros_$timestamp.csv"
                        val csvContent = generateAlarmasCsv(displayedAlarmas)
                        onExportCsv(fileName, csvContent)
                        exportStatusMessage = "Exportando $totalRecords alarmas a CSV..."
                    },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportar CSV", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Clear Records Button (Only for Administrador 1, Supervisor 2, Desarrollador 0)
                if (canClearRecords) {
                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Limpiar Registros", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Status Toast Banner
        val activeStatusMsg = exportStatusMessage ?: uiState.statusMessage
        if (activeStatusMsg != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, Color(0xFF4CAF50))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activeStatusMsg,
                            color = Color(0xFF1B5E20),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = {
                            exportStatusMessage = null
                            viewModel.clearStatusMessage()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // --- EXPANDABLE FILTERS SECTION ---
        AnimatedVisibility(visible = showFiltersPanel) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Filtros de Búsqueda:",
                        color = CamionColors.headerBackground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Filter Fecha Desde
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .clickable { showDatePickerFor = "DESDE" }
                        ) {
                            OutlinedTextField(
                                value = uiState.filterFechaDesde,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Desde (Fecha)", fontSize = 12.sp) },
                                placeholder = { Text("YYYY-MM-DD", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Fecha Desde",
                                        tint = CamionColors.headerBackground,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (uiState.filterFechaDesde.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setFilterFechaDesde("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Limpiar fecha desde",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color(0xFF1A1A1A),
                                    disabledBorderColor = Color(0xFFB0BEC5),
                                    disabledLabelColor = CamionColors.headerBackground,
                                    disabledLeadingIconColor = CamionColors.headerBackground,
                                    disabledPlaceholderColor = Color.Gray,
                                    disabledContainerColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Filter Fecha Hasta
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .clickable { showDatePickerFor = "HASTA" }
                        ) {
                            OutlinedTextField(
                                value = uiState.filterFechaHasta,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Hasta (Fecha)", fontSize = 12.sp) },
                                placeholder = { Text("YYYY-MM-DD", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Fecha Hasta",
                                        tint = CamionColors.headerBackground,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (uiState.filterFechaHasta.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setFilterFechaHasta("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Limpiar fecha hasta",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color(0xFF1A1A1A),
                                    disabledBorderColor = Color(0xFFB0BEC5),
                                    disabledLabelColor = CamionColors.headerBackground,
                                    disabledLeadingIconColor = CamionColors.headerBackground,
                                    disabledPlaceholderColor = Color.Gray,
                                    disabledContainerColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Filter by Placa / Vehículo
                        OutlinedTextField(
                            value = uiState.filterPlaca,
                            onValueChange = { viewModel.setFilterPlaca(it) },
                            label = { Text("Placa", fontSize = 12.sp) },
                            placeholder = { Text("Ej. ABC123", fontSize = 12.sp) },
                            trailingIcon = {
                                if (uiState.filterPlaca.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setFilterPlaca("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpiar placa",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.0f),
                            singleLine = true
                        )

                        // Filter by Estado Chips
                        Row(
                            modifier = Modifier.weight(1.6f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("TODOS", "ACTIVA", "INACTIVA").forEach { estado ->
                                val selected = uiState.filterEstado.equals(estado, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setFilterEstado(estado) },
                                    label = {
                                        Text(
                                            text = estado,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color(0xFFF0F4F8),
                                        labelColor = Color(0xFF333333),
                                        selectedContainerColor = CamionColors.headerBackground,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Clear All Filters Button
                        if (uiState.filterFechaDesde.isNotEmpty() || uiState.filterFechaHasta.isNotEmpty() || uiState.filterPlaca.isNotEmpty() || uiState.filterEstado != "TODOS") {
                            TextButton(onClick = { viewModel.clearFilters() }) {
                                Text("Limpiar Filtros", color = Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- CONTENT / DATA TABLE SECTION ---
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF005AC1))
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.error}", color = Color.Red)
            }
        } else {
            // Table Header with Column Sorting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF265D82))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SortableAlarmaHeaderItem(
                    title = "Placa",
                    column = AlarmaSortColumn.PLACA,
                    currentColumn = uiState.sortColumn,
                    direction = uiState.sortDirection,
                    onSort = { viewModel.toggleSort(it) },
                    modifier = Modifier.weight(1.2f)
                )
                SortableAlarmaHeaderItem(
                    title = "Alarma",
                    column = AlarmaSortColumn.ALARMA,
                    currentColumn = uiState.sortColumn,
                    direction = uiState.sortDirection,
                    onSort = { viewModel.toggleSort(it) },
                    modifier = Modifier.weight(2.2f)
                )
                SortableAlarmaHeaderItem(
                    title = "Inicio",
                    column = AlarmaSortColumn.INICIO,
                    currentColumn = uiState.sortColumn,
                    direction = uiState.sortDirection,
                    onSort = { viewModel.toggleSort(it) },
                    modifier = Modifier.weight(1.8f)
                )
                SortableAlarmaHeaderItem(
                    title = "Fin",
                    column = AlarmaSortColumn.FIN,
                    currentColumn = uiState.sortColumn,
                    direction = uiState.sortDirection,
                    onSort = { viewModel.toggleSort(it) },
                    modifier = Modifier.weight(1.8f)
                )
                SortableAlarmaHeaderItem(
                    title = "Estado",
                    column = AlarmaSortColumn.ESTADO,
                    currentColumn = uiState.sortColumn,
                    direction = uiState.sortDirection,
                    onSort = { viewModel.toggleSort(it) },
                    modifier = Modifier.weight(1.0f)
                )
            }

            if (displayedAlarmas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (uiState.alarmas.isEmpty()) "No hay alarmas registradas en el histórico." else "No se encontraron alarmas con los filtros seleccionados.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(paginatedAlarmas) { alarma ->
                        AlarmaRegistroRow(alarma)
                    }
                }

                PaginationBar(
                    currentPage = validCurrentPage,
                    totalPages = totalPages,
                    pageSize = pageSize,
                    totalRecords = totalRecords,
                    fromRecord = fromRecord,
                    toRecord = toRecord,
                    pageSizeOptions = pageSizeOptions,
                    onPageChange = { newPage ->
                        currentPage = newPage
                    },
                    onPageSizeChange = { newSize ->
                        pageSize = newSize
                        currentPage = 1
                    }
                )
            }
        }
    }

    // --- DATE PICKER DIALOG ---
    if (showDatePickerFor != null) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePickerFor = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            sdf.timeZone = TimeZone.getTimeZone("UTC")
                            val formatted = sdf.format(Date(millis))
                            if (showDatePickerFor == "DESDE") {
                                viewModel.setFilterFechaDesde(formatted)
                            } else if (showDatePickerFor == "HASTA") {
                                viewModel.setFilterFechaHasta(formatted)
                            }
                        }
                        showDatePickerFor = null
                    }
                ) {
                    Text("Aceptar", fontWeight = FontWeight.Bold, color = CamionColors.headerBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerFor = null }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- CONFIRMATION DIALOG: CLEAR RECORDS ---
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "¿Limpiar histórico de alarmas?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Esta acción eliminará de forma permanente todos los registros históricos de alarmas en la base de datos local. Esta acción no se puede deshacer.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        if (currentUser != null) {
                            viewModel.clearAllAlarmasRegistros(currentUser)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun AlarmaRegistroRow(alarma: AlarmaRegistroUiModel) {
    Card(
        shape = RoundedCornerShape(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (alarma.placa.isNotBlank()) alarma.placa else "V-${alarma.idVehiculo}",
                color = Color(0xFF1A1A1A),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.2f)
            )
            Text(
                text = alarma.alarmaDescripcion,
                color = Color(0xFF1A1A1A),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(2.2f)
            )
            Text(
                text = alarma.horaInicio,
                color = Color(0xFF1A1A1A),
                fontSize = 13.sp,
                modifier = Modifier.weight(1.8f)
            )
            Text(
                text = if (alarma.horaFin.isBlank()) "-" else alarma.horaFin,
                color = Color(0xFF1A1A1A),
                fontSize = 13.sp,
                modifier = Modifier.weight(1.8f)
            )
            Text(
                text = alarma.estado ?: "Pendiente",
                color = if (alarma.estado == "Activa") Color(0xFFC62828) else Color(0xFF2E7D32),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.0f)
            )
        }
    }
}

@Composable
private fun SortableAlarmaHeaderItem(
    title: String,
    column: AlarmaSortColumn,
    currentColumn: AlarmaSortColumn,
    direction: SortDirection,
    onSort: (AlarmaSortColumn) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable { onSort(column) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        val arrow = when {
            currentColumn != column || direction == SortDirection.NONE -> "↕"
            direction == SortDirection.ASC -> "▲"
            else -> "▼"
        }
        val arrowAlpha = if (currentColumn == column && direction != SortDirection.NONE) 1.0f else 0.4f
        Text(
            text = arrow,
            color = Color.White.copy(alpha = arrowAlpha),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun generateAlarmasCsv(alarmas: List<AlarmaRegistroUiModel>): String {
    val sb = StringBuilder()
    sb.appendLine("Placa,IdAlarma,Alarma,IdVehiculo,IdViaje,HoraInicio,HoraFin,Estado")
    alarmas.forEach { a ->
        val safeDesc = a.alarmaDescripcion.replace("\"", "\"\"")
        val safePlaca = a.placa.replace("\"", "\"\"")
        val fin = if (a.horaFin.isBlank()) "-" else a.horaFin
        sb.appendLine("\"$safePlaca\",${a.idAlarma},\"$safeDesc\",${a.idVehiculo},${a.idViaje},\"${a.horaInicio}\",\"$fin\",\"${a.estado ?: ""}\"")
    }
    return sb.toString()
}

@Composable
private fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    pageSize: Int,
    totalRecords: Int,
    fromRecord: Int,
    toRecord: Int,
    pageSizeOptions: List<Int>,
    onPageChange: (Int) -> Unit,
    onPageSizeChange: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = CamionColors.headerBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Summary Text
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Mostrando $fromRecord - $toRecord de $totalRecords registros",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Center: Page Size Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Filas:",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp
                )
                pageSizeOptions.forEach { size ->
                    val isSelected = size == pageSize
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = if (isSelected) Color(0xFF005AC1) else Color(0xFF0A1E34),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF64B5F6) else Color(0xFF1E3A5F)),
                        modifier = Modifier
                            .clickable { onPageSizeChange(size) }
                            .padding(1.dp)
                    ) {
                        Text(
                            text = size.toString(),
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Right: Navigation Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // First Page (|<)
                Button(
                    onClick = { onPageChange(1) },
                    enabled = currentPage > 1,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF005AC1),
                        disabledContainerColor = Color(0xFF1E3A5F).copy(alpha = 0.5f)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("|<", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (currentPage > 1) Color.White else Color.Gray)
                }

                // Prev Page (<)
                Button(
                    onClick = { onPageChange(currentPage - 1) },
                    enabled = currentPage > 1,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF005AC1),
                        disabledContainerColor = Color(0xFF1E3A5F).copy(alpha = 0.5f)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("< Ant", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (currentPage > 1) Color.White else Color.Gray)
                }

                // Page Indicator Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF0A1E34),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                    modifier = Modifier.height(30.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Text(
                            text = "Pág $currentPage de $totalPages",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Next Page (>)
                Button(
                    onClick = { onPageChange(currentPage + 1) },
                    enabled = currentPage < totalPages,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF005AC1),
                        disabledContainerColor = Color(0xFF1E3A5F).copy(alpha = 0.5f)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Sig >", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (currentPage < totalPages) Color.White else Color.Gray)
                }

                // Last Page (>|)
                Button(
                    onClick = { onPageChange(totalPages) },
                    enabled = currentPage < totalPages,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF005AC1),
                        disabledContainerColor = Color(0xFF1E3A5F).copy(alpha = 0.5f)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(">|", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (currentPage < totalPages) Color.White else Color.Gray)
                }
            }
        }
    }
}
