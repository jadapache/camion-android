package com.pronavicola.camion.presentation.registros

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.ceil
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.presentation.theme.CamionColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrosScreen(
    currentUser: SessionUser? = null,
    registros: List<VariableRegistroEntity>,
    deviceId: String = "ABC123",
    onResendRegistro: (VariableRegistroEntity) -> Unit = {},
    onClearAllRegistros: () -> Unit = {},
    onLoadPage: ((page: Int, pageSize: Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    var selectedRegistroForPayload by remember { mutableStateOf<VariableRegistroEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }

    // Filtering State
    var filterFechaDesde by remember { mutableStateOf("") }
    var filterFechaHasta by remember { mutableStateOf("") }
    var showDatePickerFor by remember { mutableStateOf<String?>(null) } // "DESDE", "HASTA" or null

    var filterPlaca by remember { mutableStateOf("") }
    var filterEstado by remember { mutableStateOf("TODOS") } // TODOS, ENVIADO, PENDIENTE
    var showFiltersPanel by remember { mutableStateOf(false) }

    // Sorting State
    var sortColumn by remember { mutableStateOf(SortColumn.NONE) }
    var sortDirection by remember { mutableStateOf(SortDirection.NONE) }

    // Pagination & Lazy Loading State
    var currentPage by remember { mutableStateOf(1) }
    var pageSize by remember { mutableStateOf(25) }
    val pageSizeOptions = remember { listOf(15, 25, 50, 100) }

    // Reset page to 1 whenever filters, sorting, or pageSize changes
    LaunchedEffect(filterFechaDesde, filterFechaHasta, filterPlaca, filterEstado, sortColumn, sortDirection, pageSize) {
        currentPage = 1
    }

    // Roles with permission to clean records: Desarrollador (0), Administrador (1), Supervisor (2)
    val canClearRecords = currentUser == null || currentUser.idPerfil in listOf(0, 1, 2)

    fun toggleSort(column: SortColumn) {
        if (sortColumn == column) {
            sortDirection = when (sortDirection) {
                SortDirection.NONE -> SortDirection.ASC
                SortDirection.ASC -> SortDirection.DESC
                SortDirection.DESC -> SortDirection.NONE
            }
            if (sortDirection == SortDirection.NONE) {
                sortColumn = SortColumn.NONE
            }
        } else {
            sortColumn = column
            sortDirection = SortDirection.ASC
        }
    }

    // Filter & Sort Pipeline
    val filteredRegistros = remember(
        registros,
        filterFechaDesde,
        filterFechaHasta,
        filterPlaca,
        filterEstado,
        sortColumn,
        sortDirection
    ) {
        val list = registros.filter { reg ->
            val datePart = reg.fecha.substringBefore(" ").trim()
            val matchDesde = filterFechaDesde.isBlank() || datePart >= filterFechaDesde
            val matchHasta = filterFechaHasta.isBlank() || datePart <= filterFechaHasta
            val matchPlaca = filterPlaca.isBlank() || reg.idVehiculo.toString().contains(filterPlaca.trim(), ignoreCase = true)
            val matchEstado = when (filterEstado) {
                "ENVIADO" -> reg.estadoEnvio.equals("ENVIADO", ignoreCase = true)
                "PENDIENTE" -> !reg.estadoEnvio.equals("ENVIADO", ignoreCase = true)
                else -> true
            }
            matchDesde && matchHasta && matchPlaca && matchEstado
        }

        when (sortColumn) {
            SortColumn.FECHA -> if (sortDirection == SortDirection.ASC) list.sortedBy { it.fecha } else if (sortDirection == SortDirection.DESC) list.sortedByDescending { it.fecha } else list
            SortColumn.T1 -> if (sortDirection == SortDirection.ASC) list.sortedBy { it.temperatura1 } else if (sortDirection == SortDirection.DESC) list.sortedByDescending { it.temperatura1 } else list
            SortColumn.T2 -> if (sortDirection == SortDirection.ASC) list.sortedBy { it.temperatura2 } else if (sortDirection == SortDirection.DESC) list.sortedByDescending { it.temperatura2 } else list
            SortColumn.HUM -> if (sortDirection == SortDirection.ASC) list.sortedBy { it.humedad } else if (sortDirection == SortDirection.DESC) list.sortedByDescending { it.humedad } else list
            SortColumn.TENSION -> if (sortDirection == SortDirection.ASC) list.sortedBy { it.tension } else if (sortDirection == SortDirection.DESC) list.sortedByDescending { it.tension } else list
            SortColumn.ESTADO -> if (sortDirection == SortDirection.ASC) list.sortedBy { it.estadoEnvio } else if (sortDirection == SortDirection.DESC) list.sortedByDescending { it.estadoEnvio } else list
            SortColumn.NONE -> list
        }
    }

    val totalRecords = filteredRegistros.size
    val totalPages = remember(totalRecords, pageSize) {
        maxOf(1, ceil(totalRecords.toDouble() / pageSize).toInt())
    }

    LaunchedEffect(totalPages) {
        if (currentPage > totalPages) {
            currentPage = totalPages
        }
    }

    val validCurrentPage = currentPage.coerceIn(1, totalPages)

    // Paginated / Lazy slice of records for the active page
    val paginatedRegistros = remember(filteredRegistros, validCurrentPage, pageSize) {
        val offset = (validCurrentPage - 1) * pageSize
        filteredRegistros.drop(offset).take(pageSize)
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
                    text = "HISTORICO DE REGISTROS",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$totalRecords de ${registros.size} registros encontrados • Pág $validCurrentPage de $totalPages",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
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

                // Export CSV Button
                Button(
                    onClick = {
                        val csv = generateCsv(filteredRegistros)
                        clipboardManager.setText(AnnotatedString(csv))
                        exportStatusMessage = "CSV con ${filteredRegistros.size} registros copiado al portapapeles."
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

                // Clear Records Button (Only for Admin, Supervisor, Desarrollador)
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

        // Status Toast Banner (e.g., CSV export confirmation)
        if (exportStatusMessage != null) {
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
                            text = exportStatusMessage ?: "",
                            color = Color(0xFF1B5E20),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = { exportStatusMessage = null },
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
                        // Filter Fecha Desde (Selectable Date Component)
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .clickable { showDatePickerFor = "DESDE" }
                        ) {
                            OutlinedTextField(
                                value = filterFechaDesde,
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
                                    if (filterFechaDesde.isNotEmpty()) {
                                        IconButton(onClick = { filterFechaDesde = "" }) {
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

                        // Filter Fecha Hasta (Selectable Date Component)
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .clickable { showDatePickerFor = "HASTA" }
                        ) {
                            OutlinedTextField(
                                value = filterFechaHasta,
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
                                    if (filterFechaHasta.isNotEmpty()) {
                                        IconButton(onClick = { filterFechaHasta = "" }) {
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
                            value = filterPlaca,
                            onValueChange = { filterPlaca = it },
                            label = { Text("Placa", fontSize = 12.sp) },
                            placeholder = { Text("Ej. ABC123", fontSize = 12.sp) },
                            trailingIcon = {
                                if (filterPlaca.isNotEmpty()) {
                                    IconButton(onClick = { filterPlaca = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpiar placa",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(0.9f),
                            singleLine = true
                        )

                        // Filter by Estado Chips
                        Row(
                            modifier = Modifier.weight(1.4f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("TODOS", "ENVIADO", "PENDIENTE").forEach { estado ->
                                val selected = filterEstado == estado
                                FilterChip(
                                    selected = selected,
                                    onClick = { filterEstado = estado },
                                    label = {
                                        Text(
                                            text = when (estado) {
                                                "TODOS" -> "Todos"
                                                "ENVIADO" -> "Enviados"
                                                "PENDIENTE" -> "Pendientes"
                                                else -> estado
                                            },
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
                    }
                }
            }
        }

        // --- TABLE HEADER WITH SORTING ---
        TableHeader(
            sortColumn = sortColumn,
            sortDirection = sortDirection,
            onSort = ::toggleSort
        )

        // --- TABLE BODY / ROWS ---
        if (filteredRegistros.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (registros.isEmpty()) "No hay registros guardados en el historial." else "No hay registros que coincidan con los filtros aplicados.",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(paginatedRegistros, key = { it.idRegistro }) { registro ->
                    RegistroRow(
                        registro = registro,
                        onViewPayload = { selectedRegistroForPayload = registro },
                        onResend = { onResendRegistro(registro) }
                    )
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
                    onLoadPage?.invoke(newPage, pageSize)
                },
                onPageSizeChange = { newSize ->
                    pageSize = newSize
                    currentPage = 1
                    onLoadPage?.invoke(1, newSize)
                }
            )
        }
    }

    // --- MODAL: SELECTABLE DATE PICKER DIALOG ---
    showDatePickerFor?.let { pickerTarget ->
        val initialMillis = when (pickerTarget) {
            "DESDE" -> dateStringToMillis(filterFechaDesde)
            "HASTA" -> dateStringToMillis(filterFechaHasta)
            else -> null
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerFor = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val formatted = millisToDateString(millis)
                            if (pickerTarget == "DESDE") {
                                filterFechaDesde = formatted
                            } else if (pickerTarget == "HASTA") {
                                filterFechaHasta = formatted
                            }
                        }
                        showDatePickerFor = null
                    }
                ) {
                    Text("Aceptar", fontWeight = FontWeight.Bold)
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

    // --- MODAL: CONFIRM DELETE ALL RECORDS ---
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = Color(0xFFB71C1C),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Limpiar Historial de Registros",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = "¿Está seguro de que desea eliminar todos los registros históricos almacenados localmente? Esta operación es irreversible.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllRegistros()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) {
                    Text("Eliminar Todos", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(8.dp)
        )
    }

    // --- MODAL: JSON PAYLOAD VIEWER ---
    selectedRegistroForPayload?.let { reg ->
        JsonPayloadDialog(
            registro = reg,
            deviceId = deviceId,
            onDismiss = { selectedRegistroForPayload = null },
            onResend = {
                onResendRegistro(reg)
                selectedRegistroForPayload = null
            }
        )
    }
}

@Composable
private fun TableHeader(
    sortColumn: SortColumn,
    sortDirection: SortDirection,
    onSort: (SortColumn) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CamionColors.headerBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SortableHeaderItem(
            title = "Fecha/Hora",
            column = SortColumn.FECHA,
            currentColumn = sortColumn,
            direction = sortDirection,
            onSort = onSort,
            modifier = Modifier.weight(2.0f)
        )
        SortableHeaderItem(
            title = "T1",
            column = SortColumn.T1,
            currentColumn = sortColumn,
            direction = sortDirection,
            onSort = onSort,
            modifier = Modifier.weight(1.0f)
        )
        SortableHeaderItem(
            title = "T2",
            column = SortColumn.T2,
            currentColumn = sortColumn,
            direction = sortDirection,
            onSort = onSort,
            modifier = Modifier.weight(1.0f)
        )
        SortableHeaderItem(
            title = "Hum",
            column = SortColumn.HUM,
            currentColumn = sortColumn,
            direction = sortDirection,
            onSort = onSort,
            modifier = Modifier.weight(1.0f)
        )
        SortableHeaderItem(
            title = "Tensión",
            column = SortColumn.TENSION,
            currentColumn = sortColumn,
            direction = sortDirection,
            onSort = onSort,
            modifier = Modifier.weight(1.0f)
        )
        SortableHeaderItem(
            title = "Estado",
            column = SortColumn.ESTADO,
            currentColumn = sortColumn,
            direction = sortDirection,
            onSort = onSort,
            modifier = Modifier.weight(1.0f)
        )
        Text(
            text = "Acciones",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.weight(1.0f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SortableHeaderItem(
    title: String,
    column: SortColumn,
    currentColumn: SortColumn,
    direction: SortDirection,
    onSort: (SortColumn) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = currentColumn == column
    Row(
        modifier = modifier
            .clickable { onSort(column) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = when {
                isSelected && direction == SortDirection.ASC -> Icons.Default.ArrowUpward
                isSelected && direction == SortDirection.DESC -> Icons.Default.ArrowDownward
                else -> Icons.Default.SwapVert
            },
            contentDescription = "Ordenar por $title",
            tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun RegistroRow(
    registro: VariableRegistroEntity,
    onViewPayload: () -> Unit,
    onResend: () -> Unit,
) {
    val isEnviado = registro.estadoEnvio.equals("ENVIADO", ignoreCase = true)

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
            // Fecha / Hora
            Text(
                text = registro.fecha,
                modifier = Modifier.weight(2.0f),
                fontSize = 13.sp,
                color = Color(0xFF1A1A1A),
                fontWeight = FontWeight.Normal
            )

            // T1
            Text(
                text = "${String.format(Locale.US, "%.1f", registro.temperatura1)} °C",
                modifier = Modifier.weight(1.0f),
                fontSize = 13.sp,
                color = Color(0xFF1A1A1A),
                fontWeight = FontWeight.Normal
            )

            // T2
            Text(
                text = "${String.format(Locale.US, "%.1f", registro.temperatura2)} °C",
                modifier = Modifier.weight(1.0f),
                fontSize = 13.sp,
                color = Color(0xFF1A1A1A),
                fontWeight = FontWeight.Normal
            )

            // Hum
            Text(
                text = "${String.format(Locale.US, "%.1f", registro.humedad)} %",
                modifier = Modifier.weight(1.0f),
                fontSize = 13.sp,
                color = Color(0xFF1A1A1A),
                fontWeight = FontWeight.Normal
            )

            // Tensión
            Text(
                text = "${String.format(Locale.US, "%.1f", registro.tension)} V",
                modifier = Modifier.weight(1.0f),
                fontSize = 13.sp,
                color = Color(0xFF1A1A1A),
                fontWeight = FontWeight.Normal
            )

            // Estado (Texto plano sin badges ni containers)
            Text(
                text = if (isEnviado) "Enviado" else "Pendiente",
                modifier = Modifier.weight(1.0f),
                fontSize = 13.sp,
                color = if (isEnviado) Color(0xFF007A00) else Color(0xFFB25E00),
                fontWeight = FontWeight.Medium
            )

            // Acciones (Eye + Paper Plane)
            Row(
                modifier = Modifier.weight(1.0f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: Eye (View JSON Payload)
                IconButton(
                    onClick = onViewPayload,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Ver Trama JSON",
                        tint = CamionColors.headerBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Button 2: Paper Plane (Send to MQTT if Pending, disabled if already sent)
                IconButton(
                    onClick = onResend,
                    enabled = !isEnviado,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isEnviado) "Ya enviado" else "Enviar a MQTT",
                        tint = if (!isEnviado) Color(0xFF007A00) else Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Modal Dialog that visually matches the user's reference mockup:
 * - Code bracket header with title and close 'X'
 * - Estado badge with sync status
 * - Copy JSON button
 * - Clean dark syntax card for exact payload JSON
 * - Re-enviar MQTT and Cerrar action buttons
 */
@Composable
private fun JsonPayloadDialog(
    registro: VariableRegistroEntity,
    deviceId: String,
    onDismiss: () -> Unit,
    onResend: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val isEnviado = registro.estadoEnvio.equals("ENVIADO", ignoreCase = true)
    val devId = registro.idVehiculo.toString().ifBlank { deviceId.ifBlank { "FURG-001-AND" } }
    val isoTimestamp = formatIsoTimestamp(registro.fecha)

    val jsonPayload = """{
  "device_id": "$devId",
  "temperature_c": ${registro.temperatura1},
  "temperature_2_c": ${registro.temperatura2},
  "humidity_pct": ${registro.humedad},
  "voltage_v": ${registro.tension},
  "timestamp": "$isoTimestamp"
}"""

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .width(520.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0A1220),
            border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "TRAMA JSON (REGISTRO #${registro.idRegistro})",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                // Estado Row + Copiar JSON Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Estado:",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isEnviado) Color(0xFF052354) else Color(0xFF332005),
                            border = BorderStroke(1.dp, if (isEnviado) Color(0xFF00E676) else Color(0xFFFFB74D))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isEnviado) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = if (isEnviado) Color(0xFF00E676) else Color(0xFFFFB74D),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnviado) "ENVIADO / SINCRONIZADO" else "PENDIENTE DE ENVÍO",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(jsonPayload))
                        },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0040A8))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Copiar JSON",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Subtitle
                Text(
                    text = "Payload MQTT exacto transmitido:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )

                // JSON Code Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF040811),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Text(
                        text = jsonPayload,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color(0xFF00E676),
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onResend,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RE-ENVIAR MQTT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF052354))
                    ) {
                        Text(
                            text = "CERRAR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun formatIsoTimestamp(fechaStr: String): String {
    return try {
        if (fechaStr.contains(" ") && !fechaStr.contains("T")) {
            val parts = fechaStr.split(" ")
            "${parts[0]}T${parts[1]}.000Z"
        } else if (fechaStr.contains("T")) {
            if (fechaStr.endsWith("Z")) fechaStr else "${fechaStr}Z"
        } else {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000'Z'", Locale.getDefault()).format(Date())
        }
    } catch (_: Exception) {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000'Z'", Locale.getDefault()).format(Date())
    }
}

private fun generateCsv(data: List<VariableRegistroEntity>): String {
    val sb = StringBuilder()
    sb.append("IdRegistro,IdVehiculo,Fecha,Temperatura1,Temperatura2,Humedad,Tension,EstadoEnvio\n")
    data.forEach { r ->
        sb.append("${r.idRegistro},\"${r.idVehiculo}\",\"${r.fecha}\",${r.temperatura1},${r.temperatura2},${r.humedad},${r.tension},\"${r.estadoEnvio}\"\n")
    }
    return sb.toString()
}

private fun millisToDateString(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date(millis))
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Por pág:",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
                pageSizeOptions.forEach { size ->
                    val isSelected = size == pageSize
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) Color(0xFF005AC1) else Color(0xFF1E3A5F),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF64B5F6) else Color.Transparent),
                        modifier = Modifier.clickable { onPageSizeChange(size) }
                    ) {
                        Text(
                            text = size.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Right: Navigation Controls
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
                        modifier = Modifier.padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pág $currentPage / $totalPages",
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
