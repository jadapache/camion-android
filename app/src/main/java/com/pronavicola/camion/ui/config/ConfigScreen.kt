package com.pronavicola.camion.ui.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onUserSelected: (String) -> Unit,
    onLoginPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onToggleChangePassword: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedUser = users.firstOrNull { it.idUsuario == selectedUserId }
    val isSameUser = selectedUserId == currentUser.idUsuario

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CamionColors.panelBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "CONFIGURACION / USUARIO",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "Usuario actual: ${currentUser.usuario} (Perfil ${currentUser.idPerfil})",
            color = Color.White,
            fontWeight = FontWeight.Medium,
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = selectedUser?.usuario ?: "Seleccionar usuario",
                onValueChange = {},
                readOnly = true,
                label = { Text("Usuario") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                users.forEach { user ->
                    DropdownMenuItem(
                        text = { Text("${user.usuario} (${user.idUsuario})") },
                        onClick = {
                            onUserSelected(user.idUsuario)
                            expanded = false
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            value = loginPassword,
            onValueChange = onLoginPasswordChange,
            label = { Text("CONTRASEÑA") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        if (isSameUser) {
            TextButton(onClick = onToggleChangePassword) {
                Text(
                    text = if (showChangePassword) "Cancelar cambio de contraseña" else "Cambiar contraseña",
                    color = Color.White,
                )
            }
        }

        if (showChangePassword && isSameUser) {
            OutlinedTextField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                label = { Text("NUEVA CONTRASEÑA") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(onClick = onConfirm) {
                Text(if (showChangePassword) "GUARDAR PASSWORD" else "CAMBIAR USUARIO")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Perfiles: 1=Admin, 2=Supervisor, 3=Tecnico, 4=Operario. " +
                "Menor numero = mayor privilegio. Cambio a perfil superior no requiere contraseña.",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
        )
    }
}
