package com.pronavicola.camion.ui.registros

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.ui.theme.CamionColors

@Composable
fun RegistrosScreen(
    registros: List<VariableRegistroEntity>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CamionColors.panelBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "HISTORIAL DE VARIABLES",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TableHeader()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(registros) { registro ->
                RegistroRow(registro)
            }
        }
    }
}

@Composable
private fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CamionColors.headerBackground)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderText("Fecha/Hora", Modifier.weight(1.5f))
        HeaderText("T1", Modifier.weight(1f))
        HeaderText("T2", Modifier.weight(1f))
        HeaderText("Hum", Modifier.weight(1f))
    }
}

@Composable
private fun RowScope.HeaderText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = modifier
    )
}

@Composable
private fun RegistroRow(registro: VariableRegistroEntity) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(registro.fecha, modifier = Modifier.weight(1.5f), fontSize = 13.sp)
            Text("${registro.temperatura1}°C", modifier = Modifier.weight(1f), fontSize = 13.sp)
            Text("${registro.temperatura2}°C", modifier = Modifier.weight(1f), fontSize = 13.sp)
            Text("${registro.humedad}%", modifier = Modifier.weight(1f), fontSize = 13.sp)
        }
    }
}
