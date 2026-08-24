package com.pronavicola.camion.ui.hmi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronavicola.camion.R
import com.pronavicola.camion.domain.model.HmiState
import com.pronavicola.camion.domain.model.ProcesoEstado
import com.pronavicola.camion.domain.model.ZoneStatus
import com.pronavicola.camion.ui.theme.CamionColors
import java.util.Locale

@Composable
fun HmiScreen(
    state: HmiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CamionColors.panelBackground)
    ) {
        // Main HMI Area - Filling the whole container
        TruckHmiLayout(state = state)

        // Hovering Info Card (Top Left)
        Card(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "REGISTRO AUTOMÁTICO",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.DarkGray.copy(alpha = 0.8f)
                )
                ConnectionBadge(conectado = state.conectado)
            }
        }

    }
}

@Composable
private fun TruckHmiLayout(state: HmiState) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = this.maxWidth
        val h = this.maxHeight

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.10f
                    scaleY = 1.35f
                    translationY = size.height * 0.12f
                }
        ) {
            // SVG Truck Image - Using FillBounds to fill the entire space
            Image(
                painter = painterResource(id = R.drawable.ic_truck_silhouette),
                contentDescription = "Truck",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            // Overlay Components
            
            // ZONA 6 (Top Right)
            ZoneIndicator(
                label = "ZONA 6",
                status = state.zonas.getOrElse(5) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.71f, y = h * 0.23f)
            )
            // ZONA 5 (Top Middle)
            ZoneIndicator(
                label = "ZONA 5",
                status = state.zonas.getOrElse(4) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.61f, y = h * 0.21f)
            )
            // ZONA 4 (Top Left of trailer)
            ZoneIndicator(
                label = "ZONA 4",
                status = state.zonas.getOrElse(3) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.51f, y = h * 0.21f)
            )

            // ZONA 3 (Front Top of trailer)
            ZoneIndicator(
                label = "ZONA 3",
                status = state.zonas.getOrElse(2) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.41f, y = h * 0.23f)
            )

            // ZONA 3 (Rear Top of trailer) - Added second object for same zone
            ZoneIndicator(
                label = "ZONA 3",
                status = state.zonas.getOrElse(2) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.81f, y = h * 0.23f)
            )

            // ZONA 1 & 2
            Column(
                modifier = Modifier.offset(x = w * 0.35f, y = h * 0.32f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ZoneIndicator(label = "ZONA 1", status = state.zonas.getOrElse(0) { ZoneStatus.OFF })
                ZoneIndicator(label = "ZONA 2", status = state.zonas.getOrElse(1) { ZoneStatus.OFF })
            }

            // Irrigator (Aspersor)
            IrrigatorIndicator(
                status = state.estadoIrrigador,
                modifier = Modifier.offset(x = w * 0.35f, y = h * 0.53f)
            )

            // Main Chart Area
            Column(
                modifier = Modifier
                    .offset(x = w * 0.41f, y = h * 0.30f)
                    .width(w * 0.51f)
                    .height(h * 0.27f)
                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                    .border(0.dp, Color.Gray, RoundedCornerShape(2.dp))
                    .padding(2.dp)
            ) {
                SensorDisplayHeader(state)
                Spacer(modifier = Modifier.height(2.dp))
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    HmiLinesChart(state.history)
                }
            }
            
            // Brand Logo Area
            Box(
                modifier = Modifier
                    .offset(x = w * 0.50f, y = h * 0.65f)
                    .size(width = w * 0.15f, height = h * 0.06f),
                contentAlignment = Alignment.Center
            ) {
                Text("METALTECO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            // Truck Plate Rectangle below window
            Box(
                modifier = Modifier
                    .offset(x = w * 0.13f, y = h * 0.49f)
                    .width(w * 0.08f)
                    .background(Color.Transparent, RoundedCornerShape(1.dp))
                    .border(0.dp, Color.Gray, RoundedCornerShape(1.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.idVehiculo,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun SensorDisplayHeader(state: HmiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF37474F))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SensorLabelValue("T1", state.temperatura1, Color.Red)
        SensorLabelValue("T2", state.temperatura2, Color.Yellow)
        SensorLabelValue("HUM", state.humedad, Color.Cyan)
    }
}

@Composable
private fun SensorLabelValue(label: String, value: Double?, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label: ${value?.let { String.format(Locale.US, "%.1f", it) } ?: "--"}",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HmiLinesChart(history: List<com.pronavicola.camion.domain.model.HmiHistoryPoint>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val maxPoints = 50
        
        // Grid Lines
        for (i in 0..5) {
            val y = height - (i * 20f / 100f * height)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 0.5.dp.toPx()
            )
        }

        if (history.size < 2) return@Canvas

        val stepX = width / (maxPoints - 1)
        
        fun drawSeries(values: List<Double>, color: Color) {
            val path = Path()
            values.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height - (value.coerceIn(0.0, 100.0).toFloat() / 100f * height)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                
                drawCircle(color, 1.5.dp.toPx(), Offset(x, y))
            }
            drawPath(path, color, style = Stroke(width = 1.dp.toPx()))
        }

        drawSeries(history.map { it.hum }, Color(0xFF03A9F4))
        drawSeries(history.map { it.t2 }, Color(0xFFFFEB3B))
        drawSeries(history.map { it.t1 }, Color(0xFFF44336))
    }
}

@Composable
private fun ZoneIndicator(label: String, status: ZoneStatus, modifier: Modifier = Modifier) {
    val iconRes = when (status) {
        ZoneStatus.ON -> R.drawable.ic_ventilador_on
        ZoneStatus.OFF -> R.drawable.ic_ventilador_off
        ZoneStatus.FAIL -> R.drawable.ic_ventilador_out
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(label, color = Color.Black, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(width = 44.dp, height = 14.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun IrrigatorIndicator(status: ZoneStatus, modifier: Modifier = Modifier) {
    val iconRes = when (status) {
        ZoneStatus.ON -> R.drawable.ic_aspersor_on
        ZoneStatus.OFF -> R.drawable.ic_aspersor_off
        ZoneStatus.FAIL -> R.drawable.ic_aspersor_out
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            contentScale = ContentScale.Fit
        )
        Text("BOMBA", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ConnectionBadge(conectado: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (conectado) CamionColors.connected else CamionColors.disconnected),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (conectado) "En Línea" else "Desconectado",
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = if (conectado) CamionColors.connected else CamionColors.disconnected,
        )
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 480)
@Composable
fun HmiScreenPreview() {
    HmiScreen(
        state = HmiState(
            idVehiculo = "CAM01",
            conectado = true,
            procesoEstado = ProcesoEstado.ACTIVO,
            temperatura1 = 18.5,
            temperatura2 = 15.2,
            humedad = 55.0,
            zonas = listOf(
                ZoneStatus.ON,
                ZoneStatus.OFF,
                ZoneStatus.ON,
                ZoneStatus.OFF,
                ZoneStatus.ON,
                ZoneStatus.FAIL
            ),
            estadoIrrigador = ZoneStatus.ON,
            history = List(20) { i ->
                com.pronavicola.camion.domain.model.HmiHistoryPoint(
                    t1 = 15.0 + i * 0.2,
                    t2 = 14.0 + i * 0.1,
                    hum = 60.0 - i * 0.5
                )
            }
        )
    )
}
