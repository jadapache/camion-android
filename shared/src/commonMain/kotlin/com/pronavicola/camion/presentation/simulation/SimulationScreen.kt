package com.pronavicola.camion.presentation.simulation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronavicola.camion.domain.model.Micro820ModbusConstants
import com.pronavicola.camion.ui.theme.CamionColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimulationScreen(
    viewModel: SimulationViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val simState = uiState.simulationState

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "SIMULADOR MODBUS PLC",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Control desacoplado de simulación Micro820 (Rol Desarrollador)",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                )
            }

            // Server Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (simState.isRunning) Color(0x3300E676) else Color(0x33EF4444))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (simState.isRunning) Color(0xFF00E676) else Color(0xFFEF4444)),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (simState.isRunning) "SERVIDOR ACTIVO (${simState.config.port})" else "DETENIDO",
                    color = if (simState.isRunning) Color(0xFF00E676) else Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
        }

        // Notification Banner
        uiState.message?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = msg, color = Color.White, fontSize = 13.sp)
                }
            }
        }

        // Card 1: Server Control & Status
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Control del Servidor Emulado",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Modbus TCP Server en puerto 502",
                            color = Color(0xFFE2E8F0),
                            fontSize = 14.sp,
                        )
                        Text(
                            text = "Responde a FC 01, 03, 05, 06, 16 emulando PLC Allen-Bradley Micro820",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                        )
                    }

                    Button(
                        onClick = { viewModel.toggleServer(!simState.isRunning) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (simState.isRunning) Color(0xFFEF4444) else Color(0xFF00E676),
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(
                            imageVector = if (simState.isRunning) Icons.Default.Warning else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (simState.isRunning) "DETENER" else "INICIAR",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // Card 2: Simulated Dynamic Variables
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Parámetros Base de Sensores",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.baseT1Input,
                        onValueChange = { viewModel.onBaseT1Change(it) },
                        label = { Text("T1 (°C)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569),
                        ),
                    )
                    OutlinedTextField(
                        value = uiState.baseT2Input,
                        onValueChange = { viewModel.onBaseT2Change(it) },
                        label = { Text("T2 (°C)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569),
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.baseHumInput,
                        onValueChange = { viewModel.onBaseHumChange(it) },
                        label = { Text("Humedad (%)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569),
                        ),
                    )
                    OutlinedTextField(
                        value = uiState.baseVoltInput,
                        onValueChange = { viewModel.onBaseVoltChange(it) },
                        label = { Text("Tensión (V)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569),
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = uiState.isGateOpen,
                            onCheckedChange = { viewModel.toggleGate(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF38BDF8),
                                checkedTrackColor = Color(0xFF0284C7),
                            ),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (uiState.isGateOpen) "Compuerta ABIERTA (fuga térmica)" else "Compuerta CERRADA",
                            color = Color.White,
                            fontSize = 13.sp,
                        )
                    }

                    Button(
                        onClick = { viewModel.applyParameters() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("APLICAR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Card 3: Fault & Alarm Injection
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Inyección de Fallas y Alarmas Modbus",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Text(
                    text = "Activa bits en el Holding Register 40001 (ALARMAS) para probar la reacción del HMI y MQTT",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val alarms = listOf(
                        "Falla Energía" to Micro820ModbusConstants.BIT_ALARM_FALLA_ENERGIA,
                        "Zona 1" to Micro820ModbusConstants.BIT_ALARM_ZONA_1,
                        "Zona 2" to Micro820ModbusConstants.BIT_ALARM_ZONA_2,
                        "Zona 3" to Micro820ModbusConstants.BIT_ALARM_ZONA_3,
                        "Zona 4" to Micro820ModbusConstants.BIT_ALARM_ZONA_4,
                        "Zona 5" to Micro820ModbusConstants.BIT_ALARM_ZONA_5,
                        "Zona 6" to Micro820ModbusConstants.BIT_ALARM_ZONA_6,
                        "Pulverizador" to Micro820ModbusConstants.BIT_ALARM_PULVERIZADOR,
                        "Damper" to Micro820ModbusConstants.BIT_ALARM_DAMPER,
                        "Calefacción" to Micro820ModbusConstants.BIT_ALARM_CALEFACCION,
                        "Baja Tensión" to Micro820ModbusConstants.BIT_ALARM_BAJA_TENSION,
                    )

                    alarms.forEach { (name, bit) ->
                        val isAlarmActive = uiState.activeAlarmBits.contains(bit)
                        FilterChip(
                            selected = isAlarmActive,
                            onClick = { viewModel.toggleAlarmBit(bit) },
                            label = {
                                Text(
                                    text = name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isAlarmActive) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFEF4444),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF334155),
                                labelColor = Color(0xFFCBD5E1),
                            ),
                        )
                    }
                }
            }
        }
    }
}
