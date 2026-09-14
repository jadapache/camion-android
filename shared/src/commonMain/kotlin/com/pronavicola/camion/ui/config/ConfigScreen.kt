package com.pronavicola.camion.ui.config

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronavicola.camion.data.remote.FurgonInfo
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.ui.theme.CamionColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    currentUser: SessionUser,
    deviceId: String,
    mqttBrokerUrl: String,
    mqttTopicRoot: String,
    mqttUsername: String,
    mqttPassword: String,
    modbusHost: String,
    modbusPort: String,
    publishIntervalS: String,
    mqttConnected: Boolean,
    modbusConnected: Boolean,
    outboxPendingCount: Int,
    serverBaseUrl: String,
    pairingCode: String,
    licenseStatus: String,
    licenseKeyInput: String,
    isPollingPairing: Boolean,
    availableFurgones: List<FurgonInfo>,
    onDeviceIdChange: (String) -> Unit,
    onMqttBrokerUrlChange: (String) -> Unit,
    onMqttTopicRootChange: (String) -> Unit,
    onMqttUsernameChange: (String) -> Unit,
    onMqttPasswordChange: (String) -> Unit,
    onModbusHostChange: (String) -> Unit,
    onModbusPortChange: (String) -> Unit,
    onPublishIntervalChange: (String) -> Unit,
    onServerBaseUrlChange: (String) -> Unit,
    onLicenseKeyInputChange: (String) -> Unit,
    onRequestPairingCode: () -> Unit,
    onCheckPairingStatus: () -> Unit,
    onActivateLicenseKey: () -> Unit,
    onFetchFurgones: () -> Unit,
    onSelectFurgon: (String) -> Unit,
    onSaveSystemSettings: () -> Unit,
    onSyncAllRegistros: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CamionColors.panelBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // --- PROFILE HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF005AC1), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentUser.usuario.take(2).uppercase(),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = currentUser.usuario.uppercase(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ID: ${currentUser.idUsuario} | Perfil: ${currentUser.idPerfil}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }

        // --- SECTION: DEVICE LICENSING & PAIRING ---
        ConfigSection(
            title = "Licenciamiento & Vinculación de Pantalla HMI",
            icon = Icons.Default.Security
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // License Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Estado de Licencia Web:",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    val (statusText, badgeColor) = when (licenseStatus) {
                        "ACTIVE" -> Pair("VINCULADO Y ACTIVO", Color(0xFF4CAF50))
                        "PENDING_PAIRING" -> Pair("PENDIENTE DE APROBACIÓN", Color(0xFFFFB74D))
                        else -> Pair("SIN VINCULAR", Color(0xFFEF5350))
                    }

                    Surface(
                        color = badgeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor)
                    ) {
                        Text(
                            text = statusText,
                            color = badgeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Server REST Base URL
                OutlinedTextField(
                    value = serverBaseUrl,
                    onValueChange = onServerBaseUrlChange,
                    label = { Text("URL SERVIDOR BACKEND REST") },
                    placeholder = { Text("http://192.168.1.50:3000") },
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Pairing PIN Code Banner
                if (pairingCode.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1722)),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "DISPOSITIVO: $deviceId",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "CÓDIGO PIN: $pairingCode",
                                color = Color(0xFF64B5F6),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "Ingrese este código PIN en la plataforma web FURGN BI para autorizar la pantalla.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Action Buttons for PIN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRequestPairingCode,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1))
                    ) {
                        Icon(imageVector = Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SOLICITAR PIN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onCheckPairingStatus,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPollingPairing) "SONDEANDO..." else "VERIFICAR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // License Key Direct Activation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = licenseKeyInput,
                        onValueChange = onLicenseKeyInputChange,
                        label = { Text("CLAVE DE REGISTRO (FRGN-XXXX-XXXX-XXXX)") },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.weight(1.8f),
                        singleLine = true
                    )

                    Button(
                        onClick = onActivateLicenseKey,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ACTIVAR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Associated Furgon Selector
                if (availableFurgones.isNotEmpty()) {
                    var furgonExpanded by remember { mutableStateOf(false) }
                    val selectedFurgon = availableFurgones.firstOrNull { it.plate == deviceId }

                    ExposedDropdownMenuBox(
                        expanded = furgonExpanded,
                        onExpandedChange = { furgonExpanded = !furgonExpanded },
                    ) {
                        OutlinedTextField(
                            value = selectedFurgon?.let { "${it.plate} (${it.brand ?: "Furgón"})" } ?: deviceId,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Furgón Asignado en Web") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = furgonExpanded) },
                            colors = outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = furgonExpanded,
                            onDismissRequest = { furgonExpanded = false },
                            modifier = Modifier.background(Color(0xFF2C2C2C))
                        ) {
                            availableFurgones.forEach { furgon ->
                                DropdownMenuItem(
                                    text = { Text("${furgon.plate} - ${furgon.brand ?: "Furgón"}", color = Color.White) },
                                    onClick = {
                                        onSelectFurgon(furgon.id)
                                        furgonExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION: LIVE STATUS & OUTBOX SYNC ---
        ConfigSection(
            title = "Estado de Comunicaciones & Cola MQTT",
            icon = Icons.Default.CloudSync
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // MQTT Status Pill
                    StatusPill(
                        label = "Broker MQTT",
                        isConnected = mqttConnected,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Modbus Status Pill
                    StatusPill(
                        label = "PLC Modbus",
                        isConnected = modbusConnected,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mensajes pendientes en cola Outbox:",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "$outboxPendingCount msg",
                        color = if (outboxPendingCount > 0) Color(0xFFFFB74D) else Color(0xFF81C784),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Button(
                    onClick = onSyncAllRegistros,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
                ) {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SINCRONIZAR Y ENVIAR REGISTROS AHORA", fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- SECTION: MQTT SETTINGS ---
        ConfigSection(
            title = "Configuración del Broker MQTT",
            icon = Icons.Default.WifiTethering
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = mqttBrokerUrl,
                    onValueChange = onMqttBrokerUrlChange,
                    label = { Text("URL DEL BROKER (Host & Puerto)") },
                    placeholder = { Text("tcp://broker.emqx.io:1883") },
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = mqttTopicRoot,
                    onValueChange = onMqttTopicRootChange,
                    label = { Text("TÓPICO RAÍZ (MQTT ROOT)") },
                    placeholder = { Text("furgones") },
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = mqttUsername,
                        onValueChange = onMqttUsernameChange,
                        label = { Text("USUARIO MQTT (Opcional)") },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.weight(1f),
                    )

                    OutlinedTextField(
                        value = mqttPassword,
                        onValueChange = onMqttPasswordChange,
                        label = { Text("CONTRASEÑA MQTT") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // --- SECTION: MODBUS & TIMING SETTINGS ---
        ConfigSection(
            title = "Configuración MODBUS TCP & Muestreo",
            icon = Icons.Default.Lan
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = modbusHost,
                        onValueChange = onModbusHostChange,
                        label = { Text("IP / HOST PLC") },
                        placeholder = { Text("192.168.1.10") },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.weight(2f),
                    )

                    OutlinedTextField(
                        value = modbusPort,
                        onValueChange = onModbusPortChange,
                        label = { Text("PUERTO") },
                        placeholder = { Text("502") },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = deviceId,
                        onValueChange = onDeviceIdChange,
                        label = { Text("ID DE VEHICULO / EQUIPO") },
                        placeholder = { Text("CAM01") },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.weight(1.5f),
                    )

                    OutlinedTextField(
                        value = publishIntervalS,
                        onValueChange = onPublishIntervalChange,
                        label = { Text("INTERVALO (s)") },
                        placeholder = { Text("5") },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                }

                Button(
                    onClick = onSaveSystemSettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GUARDAR AJUSTES DE SISTEMA", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun StatusPill(
    label: String,
    isConnected: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFE53935),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isConnected) "ONLINE" else "OFFLINE",
                    color = if (isConnected) Color(0xFF81C784) else Color(0xFFEF5350),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ConfigSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color(0xFF005AC1),
    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
    focusedLabelColor = Color(0xFF64B5F6),
    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
    cursorColor = Color(0xFF64B5F6)
)
