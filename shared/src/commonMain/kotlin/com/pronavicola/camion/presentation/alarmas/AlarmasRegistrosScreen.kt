package com.pronavicola.camion.presentation.alarmas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronavicola.camion.domain.usecase.alarmas.AlarmaRegistroUiModel
import com.pronavicola.camion.presentation.theme.CamionColors

@Composable
fun AlarmasRegistrosScreen(
    viewModel: AlarmasRegistrosViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CamionColors.panelBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "HISTÓRICO DE ALARMAS",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${uiState.alarmas.size} alarmas registradas",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF005AC1))
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.error}", color = Color.Red)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF265D82))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Alarma",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(2.0f)
                )
                Text(
                    "Inicio",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    "Fin",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    "Estado",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1.0f)
                )
            }

            if (uiState.alarmas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay alarmas registradas", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.alarmas) { alarma ->
                        AlarmaRegistroRow(alarma)
                    }
                }
            }
        }
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
                text = alarma.alarmaDescripcion,
                color = Color(0xFF1A1A1A),
                fontSize = 13.sp,
                modifier = Modifier.weight(2.0f)
            )
            Text(
                text = alarma.horaInicio,
                color = Color(0xFF1A1A1A),
                fontSize = 13.sp,
                modifier = Modifier.weight(1.5f)
            )
            Text(
                text = if (alarma.horaFin.isBlank()) "-" else alarma.horaFin,
                color = Color(0xFF1A1A1A),
                fontSize = 13.sp,
                modifier = Modifier.weight(1.5f)
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
