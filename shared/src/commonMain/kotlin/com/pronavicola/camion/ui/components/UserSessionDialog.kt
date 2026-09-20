package com.pronavicola.camion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pronavicola.camion.data.local.entity.UsuarioEntity
import com.pronavicola.camion.domain.model.SessionUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSessionDialog(
    currentUser: SessionUser,
    users: List<UsuarioEntity>,
    selectedUserId: String?,
    loginPassword: String,
    newPassword: String,
    showChangePassword: Boolean,
    onUserSelected: (String) -> Unit,
    onLoginPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onToggleChangePassword: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val dialogScrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121824)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(dialogScrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main Header
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "GESTIÓN DE SESIÓN & USUARIO",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                val selectedUser = users.firstOrNull { it.idUsuario == selectedUserId } ?: users.firstOrNull { it.idUsuario == currentUser.idUsuario }
                val isSameUser = (selectedUserId ?: currentUser.idUsuario) == currentUser.idUsuario
                var expanded by remember { mutableStateOf(false) }

                // Dropdown for User Selection
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    OutlinedTextField(
                        value = selectedUser?.let { getPerfilLabel(it) } ?: "Seleccionar usuario",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Usuario para login / cambio") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = dialogTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF2C2C2C))
                    ) {
                        users.forEach { user ->
                            DropdownMenuItem(
                                text = { Text(getPerfilLabel(user), color = Color.White) },
                                onClick = {
                                    onUserSelected(user.idUsuario)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Normal Login Password field (when NOT changing password)
                if (!showChangePassword) {
                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = onLoginPasswordChange,
                        label = { Text("CONTRASEÑA") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                onConfirm()
                            }
                        ),
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Collapsible Toggle for Changing Password (only if target user is current user)
                if (isSameUser) {
                    TextButton(
                        onClick = onToggleChangePassword,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = Color(0xFF90CAF9),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showChangePassword) "Ocultar cambio de contraseña" else "Cambiar contraseña",
                                color = Color(0xFF90CAF9),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Collapsible Change Password Panel - Vertically Stacked with Scrollbar
                if (showChangePassword && isSameUser) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B101A)),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "ACTUALIZAR CONTRASEÑA DE ${(selectedUser?.usuario ?: currentUser.usuario).uppercase()}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )

                            // Vertical Panel with Scrollbar for Password Fields
                            val passwordPanelScrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(passwordPanelScrollState),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Current Password
                                OutlinedTextField(
                                    value = loginPassword,
                                    onValueChange = onLoginPasswordChange,
                                    label = { Text("Contraseña Actual") },
                                    placeholder = { Text("Actual") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Next
                                    ),
                                    colors = dialogTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                // New Password
                                OutlinedTextField(
                                    value = newPassword,
                                    onValueChange = {
                                        onNewPasswordChange(it)
                                        passwordError = if (confirmPassword.isNotEmpty() && (it != confirmPassword)) {
                                            "Las contraseñas no coinciden"
                                        } else {
                                            null
                                        }
                                    },
                                    label = { Text("Nueva Contraseña") },
                                    placeholder = { Text("Nueva") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Next
                                    ),
                                    colors = dialogTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                // Confirm Password
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = {
                                        confirmPassword = it
                                        passwordError = if (it.isNotEmpty() && (it != newPassword)) {
                                            "Las contraseñas no coinciden"
                                        } else {
                                            null
                                        }
                                    },
                                    label = { Text("Confirmar Nueva") },
                                    placeholder = { Text("Confirmar") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            keyboardController?.hide()
                                        }
                                    ),
                                    colors = dialogTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            if (passwordError != null) {
                                Text(
                                    text = passwordError!!,
                                    color = Color(0xFFFF5252),
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = {
                                    if (newPassword == confirmPassword) {
                                        keyboardController?.hide()
                                        onConfirm()
                                    } else {
                                        passwordError = "Las contraseñas no coinciden"
                                    }
                                },
                                modifier = Modifier.align(Alignment.End),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1))
                            ) {
                                Text(
                                    text = "GUARDAR NUEVA CONTRASEÑA",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else if (!showChangePassword) {
                    // Action button for normal User Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                onConfirm()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1))
                        ) {
                            Text(
                                text = "CAMBIAR USUARIO",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getPerfilLabel(user: UsuarioEntity): String {
    val role = when (user.idPerfil) {
        0 -> "Desarrollador"
        1 -> "Administrador"
        2 -> "Supervisor"
        3 -> "Técnico"
        4 -> "Operario"
        else -> user.usuario
    }
    return "$role (Nivel ${user.idPerfil})"
}

@Composable
private fun dialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color(0xFF005AC1),
    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
    focusedLabelColor = Color(0xFF64B5F6),
    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
    cursorColor = Color(0xFF64B5F6)
)
