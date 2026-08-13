package com.pronavicola.camion.ui.parametros

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronavicola.camion.domain.model.ParametroUiModel
import com.pronavicola.camion.ui.theme.CamionColors
import java.util.Locale

@Composable
fun ParametrosScreen(
    parametros: List<ParametroUiModel>,
    onValueChange: (ParametroUiModel, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingParam by remember { mutableStateOf<ParametroUiModel?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CamionColors.panelBackground),
    ) {
        Text(
            text = "PARAMETROS EQUIPO",
            modifier = Modifier
                .fillMaxWidth()
                .background(CamionColors.headerBackground)
                .padding(vertical = 12.dp),
            textAlign = TextAlign.Center,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )

        ParametrosHeader()

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(parametros, key = { it.idParametro }) { param ->
                ParametroRow(
                    parametro = param,
                    onClick = { if (param.editable) editingParam = param },
                )
            }
        }
    }

    editingParam?.let { param ->
        ValueEditorDialog(
            parametro = param,
            onDismiss = { editingParam = null },
            onConfirm = { value ->
                onValueChange(param, value)
                editingParam = null
            },
        )
    }
}

@Composable
private fun ParametrosHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF455A64))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text("PARAMETRO", modifier = Modifier.weight(2f), color = Color.White, fontWeight = FontWeight.Bold)
        Text("VALOR", modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("UND", modifier = Modifier.weight(0.5f), color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
private fun ParametroRow(
    parametro: ParametroUiModel,
    onClick: () -> Unit,
) {
    val background = if (parametro.editable) CamionColors.editableRow else CamionColors.readOnlyRow
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable(enabled = parametro.editable, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(parametro.nombre, modifier = Modifier.weight(2f))
        Text(
            text = formatValue(parametro),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )
        Text(parametro.unidad, modifier = Modifier.weight(0.5f), textAlign = TextAlign.End)
    }
}

@Composable
private fun ValueEditorDialog(
    parametro: ParametroUiModel,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var input by remember(parametro) {
        mutableStateOf(formatValue(parametro))
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(parametro.nombre) },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Valor (${parametro.minimo} - ${parametro.maximo})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = input.replace(",", ".").toDoubleOrNull()
                    if (parsed == null) {
                        error = "Valor numerico invalido"
                        return@TextButton
                    }
                    if (parsed < parametro.minimo || parsed > parametro.maximo) {
                        error = "Fuera de rango permitido"
                        return@TextButton
                    }
                    onConfirm(parsed)
                },
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR")
            }
        },
    )
}

private fun formatValue(parametro: ParametroUiModel): String =
    if (parametro.esDecimal) {
        String.format(Locale.US, "%.2f", parametro.valor)
    } else {
        String.format(Locale.US, "%.0f", parametro.valor)
    }
