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
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "SELECCIONAR FURGÓN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
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
                            tint = Color.Gray,
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
                        color = Color(0xFFEF5350),
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
                            color = Color(0xFF64B5F6)
                        )
                    }
                } else if (availableFurgones.isNotEmpty()) {
                    Text(
                        text = "Furgones precargados de la API:",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
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
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF64B5F6),
                                unfocusedBorderColor = Color(0xFF4B5563),
                                focusedLabelColor = Color(0xFF64B5F6),
                                unfocusedLabelColor = Color(0xFF94A3B8),
                            ),
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color(0xFF1E222B))
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
                                                    color = if (isSelected) Color(0xFF64B5F6) else Color.White
                                                )
                                                furgon.brand?.let { brand ->
                                                    Text(
                                                        text = brand,
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsBus,
                                            contentDescription = null,
                                            tint = if (isSelected) Color(0xFF64B5F6) else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Seleccionado",
                                                tint = Color(0xFF64B5F6),
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
                        color = Color(0xFFFFB74D),
                        fontSize = 12.sp
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
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF64B5F6),
                            unfocusedBorderColor = Color(0xFF4B5563),
                            focusedLabelColor = Color(0xFF64B5F6),
                            unfocusedLabelColor = Color(0xFF94A3B8),
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
                Text("OK", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR")
            }
        }
    )
}
