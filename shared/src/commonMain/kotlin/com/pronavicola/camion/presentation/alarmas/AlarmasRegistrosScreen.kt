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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.domain.usecase.alarmas.AlarmaRegistroUiModel
import com.pronavicola.camion.presentation.theme.CamionColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmasRegistrosScreen(
    viewModel: AlarmasRegistrosViewModel,
    currentUser: SessionUser? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }

    // Filtering State
    var showDatePickerFor by remember { mutableStateOf<String?>(null) } // "DESDE", "HASTA" or null
    var showFiltersPanel by remember { mutableStateOf(false) }

    // Roles permitted to clean records: Desarrollador (0), Administrador (1), Supervisor (2)
    val canClearRecords = currentUser != null && currentUser.idPerfil in listOf(0, 1, 2)

    val displayedAlarmas = uiState.filteredAlarmas

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
                    text = "${displayedAlarmas.size} de ${uiState.alarmas.size} alarmas registradas",
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

                // Export CSV Button
                Button(
                    onClick = { showExportDialog = true },
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

        // Status Toast Banner (e.g. CSV exported or records cleared)
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
                                    label = { Text(estado, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        // Clear All Filters Button
                        if (uiState.filterFechaDesde.isNotEmpty() || uiState.filterFechaHasta.isNotEmpty() || uiState.filterEstado != "TODOS") {
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
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF265D82))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Id",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(0.6f)
                )
                Text(
                    "Alarma",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(2.4f)
                )
                Text(
                    "Inicio",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1.8f)
                )
                Text(
                    "Fin",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1.8f)
                )
                Text(
                    "Estado",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1.0f)
                )
            }

            if (displayedAlarmas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.alarmas.isEmpty()) "No hay alarmas registradas en el histórico" else "No se encontraron alarmas con los filtros seleccionados",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(displayedAlarmas) { alarma ->
                        AlarmaRegistroRow(alarma)
                    }
                }
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

    // --- EXPORT CSV DIALOG ---
    if (showExportDialog) {
        ExportAlarmasCsvDialog(
            alarmas = displayedAlarmas,
            onDismiss = { showExportDialog = false },
            onExportSuccess = { savedPath ->
                exportStatusMessage = "Archivo CSV guardado con éxito en: $savedPath"
                showExportDialog = false
            }
        )
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
                text = "${alarma.idAlarma}",
                color = Color(0xFF1A1A1A),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.6f)
            )
            Text(
                text = alarma.alarmaDescripcion,
                color = Color(0xFF1A1A1A),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(2.4f)
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
private fun ExportAlarmasCsvDialog(
    alarmas: List<AlarmaRegistroUiModel>,
    onDismiss: () -> Unit,
    onExportSuccess: (String) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val timestamp = remember {
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
    var fileName by remember { mutableStateOf("Alarmas_$timestamp.csv") }
    var folderPath by remember { mutableStateOf(getDefaultExportDirectory()) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    val csvContent = remember(alarmas) {
        val sb = StringBuilder()
        sb.appendLine("IdAlarma,Alarma,IdVehiculo,IdViaje,HoraInicio,HoraFin,Estado")
        alarmas.forEach { a ->
            val safeDesc = a.alarmaDescripcion.replace("\"", "\"\"")
            val fin = if (a.horaFin.isBlank()) "-" else a.horaFin
            sb.appendLine("${a.idAlarma},\"$safeDesc\",${a.idVehiculo},${a.idViaje},\"${a.horaInicio}\",\"$fin\",\"${a.estado ?: ""}\"")
        }
        sb.toString()
    }

    val previewLines = remember(csvContent) {
        csvContent.lines().take(5).joinToString("\n")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0A1220),
            border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "EXPORTAR HISTÓRICO DE ALARMAS A CSV",
                            color = Color.White,
                            fontSize = 16.sp,
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

                // Summary banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F223D), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alarmas a exportar: ${alarmas.size}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Formato: CSV (UTF-8)",
                        color = Color(0xFF64B5F6),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // File Name Input
                OutlinedTextField(
                    value = fileName,
                    onValueChange = {
                        fileName = it
                        saveError = null
                        saveSuccessMessage = null
                    },
                    label = { Text("Nombre del Archivo", fontSize = 12.sp, color = Color(0xFF90CAF9)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF64B5F6),
                        unfocusedBorderColor = Color(0xFF1E3A5F),
                        focusedContainerColor = Color(0xFF040811),
                        unfocusedContainerColor = Color(0xFF040811),
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Folder Path Input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = folderPath,
                        onValueChange = {
                            folderPath = it
                            saveError = null
                            saveSuccessMessage = null
                        },
                        label = { Text("Ruta / Carpeta de Destino", fontSize = 12.sp, color = Color(0xFF90CAF9)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF64B5F6),
                            unfocusedBorderColor = Color(0xFF1E3A5F),
                            focusedContainerColor = Color(0xFF040811),
                            unfocusedContainerColor = Color(0xFF040811),
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick directory buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rutas sugeridas:", color = Color.Gray, fontSize = 11.sp)
                        val quickPaths = getQuickDirectoryOptions()
                        quickPaths.forEach { (label, path) ->
                            Text(
                                text = "[$label]",
                                color = Color(0xFF64B5F6),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { folderPath = path }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                // CSV Preview
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Vista previa del encabezado y primeras filas:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF040811),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Text(
                            text = previewLines + if (alarmas.size > 4) "\n..." else "",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF00E676),
                            modifier = Modifier.padding(10.dp),
                            maxLines = 6
                        )
                    }
                }

                // Error Message if failed
                if (saveError != null) {
                    Text(
                        text = "Error al guardar: $saveError",
                        color = Color(0xFFEF5350),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Success Message
                if (saveSuccessMessage != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = saveSuccessMessage ?: "",
                            color = Color(0xFF00E676),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy to clipboard fallback button
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(csvContent))
                            saveSuccessMessage = "CSV copiado al portapapeles"
                            saveError = null
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E3A5F))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copiar Texto", color = Color.White, fontSize = 12.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text("Cancelar", color = Color.White, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val cleanFileName = if (fileName.endsWith(".csv", ignoreCase = true)) fileName else "$fileName.csv"
                                val result = saveCsvToFile(folderPath.trim(), cleanFileName.trim(), csvContent)
                                if (result.isSuccess) {
                                    val savedPath = result.getOrNull() ?: "$folderPath/$cleanFileName"
                                    saveSuccessMessage = "Guardado con éxito en: $savedPath"
                                    saveError = null
                                    onExportSuccess(savedPath)
                                } else {
                                    saveError = result.exceptionOrNull()?.message ?: "No se pudo escribir en el directorio seleccionado"
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar Archivo", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun getDefaultExportDirectory(): String {
    val candidates = listOf(
        "/sdcard/Download",
        "/storage/emulated/0/Download",
        System.getProperty("user.home")?.let { "$it/Downloads" },
        System.getProperty("user.home")?.let { "$it/Documents" },
        "."
    ).filterNotNull()

    for (path in candidates) {
        val f = java.io.File(path)
        if (f.exists() && f.canWrite()) {
            return f.absolutePath
        }
    }
    return candidates.firstOrNull() ?: "/sdcard/Download"
}

private fun getQuickDirectoryOptions(): List<Pair<String, String>> {
    val options = mutableListOf<Pair<String, String>>()
    val sdDownload = java.io.File("/sdcard/Download")
    if (sdDownload.exists()) {
        options.add("Descargas" to "/sdcard/Download")
    }
    val emulated = java.io.File("/storage/emulated/0/Download")
    if (emulated.exists() && !options.any { it.second == emulated.absolutePath }) {
        options.add("Descargas" to "/storage/emulated/0/Download")
    }
    val home = System.getProperty("user.home")
    if (!home.isNullOrBlank()) {
        val dl = java.io.File(home, "Downloads")
        if (dl.exists() || options.isEmpty()) {
            options.add("Descargas" to dl.absolutePath)
        }
        val docs = java.io.File(home, "Documentos")
        val docsEn = java.io.File(home, "Documents")
        if (docs.exists()) {
            options.add("Documentos" to docs.absolutePath)
        } else if (docsEn.exists()) {
            options.add("Documentos" to docsEn.absolutePath)
        }
    }
    if (options.isEmpty()) {
        options.add("Descargas" to "/sdcard/Download")
        options.add("Almacenamiento" to "/sdcard")
    }
    return options.distinctBy { it.second }
}

private fun saveCsvToFile(directoryPath: String, fileName: String, csvContent: String): Result<String> {
    return try {
        val dir = java.io.File(directoryPath)
        if (!dir.exists()) {
            val created = dir.mkdirs()
            if (!created && !dir.exists()) {
                return Result.failure(Exception("No se pudo crear la carpeta: $directoryPath"))
            }
        }
        val file = java.io.File(dir, fileName)
        file.writeText(csvContent, Charsets.UTF_8)
        Result.success(file.absolutePath)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
