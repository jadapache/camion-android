package com.pronavicola.camion.presentation.hmi.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronavicola.camion.data.remote.FurgonInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TruckAssignmentDialog(
    currentPlate: String,
    availableFurgones: List<FurgonInfo>,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirmPlate: (String) -> Unit,
    onRefreshFurgones: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var manualPlateInput by remember(currentPlate, availableFurgones) {
        val initialManual = if (availableFurgones.isEmpty() && currentPlate.isNotBlank() && currentPlate.length <= 8) {
            currentPlate
        } else {
            ""
        }
        mutableStateOf(initialManual)
    }

    var selectedPlate by remember(currentPlate, availableFurgones) {
        val initial = if (availableFurgones.isNotEmpty()) {
            availableFurgones.find { it.plate.equals(currentPlate, ignoreCase = true) }?.plate
                ?: availableFurgones.first().plate
        } else {
            if (currentPlate.isNotBlank() && currentPlate.length <= 8) currentPlate else ""
        }
        mutableStateOf(initial)
    }

    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.widthIn(min = 320.dp, max = 460.dp),
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = Color(0xFF005AC1),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "SELECCIONAR FURGÓN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF0F172A)
                    )
                }
                if (!isLoading) {
                    IconButton(
                        onClick = onRefreshFurgones,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refrescar",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp
                    )
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = Color(0xFF005AC1)
                        )
                    }
                } else if (availableFurgones.isNotEmpty()) {
                    Text(
                        text = "Furgones precargados de la API:",
                        color = Color(0xFF334155),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    val selectedFurgon = availableFurgones.find { it.plate.equals(selectedPlate, ignoreCase = true) }
                    val displayText = if (selectedFurgon != null) {
                        if (!selectedFurgon.brand.isNullOrBlank()) {
                            "${selectedFurgon.plate} (${selectedFurgon.brand})"
                        } else {
                            selectedFurgon.plate
                        }
                    } else {
                        selectedPlate.ifBlank { "Seleccionar furgón" }
                    }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = displayText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Placa del Furgón") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF005AC1),
                                unfocusedBorderColor = Color(0xFF64748B),
                                focusedLabelColor = Color(0xFF005AC1),
                                unfocusedLabelColor = Color(0xFF334155),
                                focusedTrailingIconColor = Color(0xFF005AC1),
                                unfocusedTrailingIconColor = Color(0xFF475569),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                            ),
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            availableFurgones.forEach { furgon ->
                                val isSelected = furgon.plate.equals(selectedPlate, ignoreCase = true)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = furgon.plate,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = if (isSelected) Color(0xFF005AC1) else Color(0xFF0F172A)
                                                )
                                                furgon.brand?.let { brand ->
                                                    Text(
                                                        text = brand,
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsBus,
                                            contentDescription = null,
                                            tint = if (isSelected) Color(0xFF005AC1) else Color(0xFF64748B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Seleccionado",
                                                tint = Color(0xFF005AC1),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        selectedPlate = furgon.plate
                                        expanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                } else {
                    // Fallback input if API returns empty list or offline
                    Text(
                        text = "No se encontraron furgones en la API. Ingrese la placa manualmente:",
                        color = Color(0xFFB45309),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    OutlinedTextField(
                        value = manualPlateInput,
                        onValueChange = {
                            manualPlateInput = it.uppercase().trim()
                            selectedPlate = manualPlateInput
                        },
                        label = { Text("Placa del Furgón") },
                        placeholder = { Text("Ej. ABC123") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF005AC1),
                            unfocusedBorderColor = Color(0xFF64748B),
                            focusedLabelColor = Color(0xFF005AC1),
                            unfocusedLabelColor = Color(0xFF334155),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmPlate(selectedPlate.trim().uppercase()) },
                enabled = selectedPlate.isNotBlank() && !isLoading
            ) {
                Text(
                    "OK",
                    fontWeight = FontWeight.Bold,
                    color = if (selectedPlate.isNotBlank() && !isLoading) Color(0xFF005AC1) else Color(0xFF94A3B8)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "CANCELAR",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )
            }
        }
    )
}
