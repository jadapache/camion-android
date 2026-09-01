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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronavicola.camion.data.local.entity.UsuarioEntity
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.ui.theme.CamionColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    currentUser: SessionUser,
    users: List<UsuarioEntity>,
    selectedUserId: String?,
    loginPassword: String,
    newPassword: String,
    showChangePassword: Boolean,
    deviceId: String,
    mqttBrokerUrl: String,
    onUserSelected: (String) -> Unit,
    onLoginPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onToggleChangePassword: () -> Unit,
    onConfirm: () -> Unit,
    onDeviceIdChange: (String) -> Unit,
    onMqttBrokerUrlChange: (String) -> Unit,
    onSaveSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CamionColors.panelBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // --- PROFILE HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF005AC1), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentUser.usuario.take(2).uppercase(),
                    color = Color.White,
                    fontSize = 24.sp,
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
                    text = "ID: ${currentUser.idUsuario}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
        }

        Text(
            text = "CONFIGURACIÓN DEL SISTEMA",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            ),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        // --- SECTION: USER MANAGEMENT ---
        ConfigSection(
            title = "Gestión de Usuario",
            icon = Icons.Default.AccountCircle
        ) {
            val selectedUser = users.firstOrNull { it.idUsuario == selectedUserId }
            val isSameUser = selectedUserId == currentUser.idUsuario
            var expanded by remember { mutableStateOf(false) }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    OutlinedTextField(
                        value = selectedUser?.usuario ?: "Seleccionar usuario",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Usuario para login/cambio") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF2C2C2C))
                    ) {
                        users.forEach { user ->
                            DropdownMenuItem(
                                text = { Text(user.usuario, color = Color.White) },
                                onClick = {
                                    onUserSelected(user.idUsuario)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = loginPassword,
                    onValueChange = onLoginPasswordChange,
                    label = { Text("CONTRASEÑA") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (isSameUser) {
                    TextButton(onClick = onToggleChangePassword) {
                        Text(
                            text = if (showChangePassword) "CANCELAR CAMBIO" else "CAMBIAR CONTRASEÑA",
                            color = Color(0xFF64B5F6),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (showChangePassword && isSameUser) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = onNewPasswordChange,
                        label = { Text("NUEVA CONTRASEÑA") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1))
                ) {
                    Text(if (showChangePassword) "GUARDAR PASSWORD" else "CAMBIAR USUARIO")
                }
            }
        }

        // --- SECTION: DEVICE REGISTRATION ---
        ConfigSection(
            title = "Registro de Dispositivo",
            icon = Icons.Default.Dns
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = deviceId,
                    onValueChange = onDeviceIdChange,
                    label = { Text("ID DE VEHICULO / EQUIPO") },
                    placeholder = { Text("Ej: CAM01") },
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                
                Text(
                    text = "Este ID identifica al equipo en los reportes y plataforma MQTT.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }

        // --- SECTION: MQTT SETTINGS ---
        ConfigSection(
            title = "Servidor MQTT",
            icon = Icons.Default.WifiTethering
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = mqttBrokerUrl,
                    onValueChange = onMqttBrokerUrlChange,
                    label = { Text("BROKER URL") },
                    placeholder = { Text("tcp://broker.emqx.io:1883") },
                    colors = outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = onSaveSystemSettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                ) {
                    Text("GUARDAR AJUSTES DE SISTEMA", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
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
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF005AC1),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
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
    focusedLabelColor = Color(0xFF005AC1),
    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
    cursorColor = Color(0xFF005AC1)
)
