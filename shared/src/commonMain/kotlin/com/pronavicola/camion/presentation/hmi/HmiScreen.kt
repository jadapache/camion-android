package com.pronavicola.camion.presentation.hmi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronavicola.camion.domain.model.HmiHistoryPoint
import com.pronavicola.camion.domain.model.HmiState
import com.pronavicola.camion.domain.model.ZoneStatus
import com.pronavicola.camion.presentation.theme.CamionColors
import com.pronavicola.camion.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
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

        // Connection Badge & Alarms at the bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ConnectionBadge(conectado = state.conectado)

            if (state.alarmasActivas.isNotEmpty()) {
                AlarmsBanner(alarmas = state.alarmasActivas)
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
            modifier = Modifier.fillMaxSize()
        ) {
            // SVG Truck Image - FillBounds to match layout ratios exactly
            Image(
                painter = painterResource(Res.drawable.ic_truck_silhouette),
                contentDescription = "Truck Silhouette",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            // License Plate ("AAA000") on door
            Box(
                modifier = Modifier
                    .offset(x = w * 0.095f, y = h * 0.627f)
                    .width(w * 0.088f)
                    .height(h * 0.077f)
                    .background(Color(0xFFECECEC), RoundedCornerShape(1.dp))
                    .border(1.dp, Color(0xFF888888), RoundedCornerShape(1.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.idVehiculo,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            }

            // Damper (Angled Green Bar in Front Section)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val x1 = size.width * (258f / 895f)
                val y1 = size.height * (48f / 415f)
                val x2 = size.width * (345f / 895f)
                val y2 = size.height * (122f / 415f)

                val strokeW = size.height * (14f / 415f)
                val barColor = if (state.damperActivo) Color(0xFF1B5E20) else Color(0xFF616161)

                drawLine(
                    color = barColor,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Square
                )
            }

            // ZONA 1 (Front Column Top Fan)
            ZoneIndicator(
                label = "ZONA 1",
                status = state.zonas.getOrElse(0) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.318f, y = h * 0.313f)
            )

            // ZONA 2 (Front Column Bottom Fan)
            ZoneIndicator(
                label = "ZONA 2",
                status = state.zonas.getOrElse(1) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.318f, y = h * 0.422f)
            )

            // Aspersor / Nebulizador / Irrigator ("BOMBA")
            IrrigatorIndicator(
                status = state.estadoIrrigador,
                modifier = Modifier.offset(x = w * 0.285f, y = h * 0.530f)
            )

            // Flame / Fire Icon ("Calor / Calefacción")
            FlameIndicator(
                active = state.calefaccionActiva,
                modifier = Modifier.offset(x = w * 0.320f, y = h * 0.627f)
            )

            // ZONA 3 (Front Top Fan Inside Trailer)
            ZoneIndicator(
                label = "ZONA 3",
                status = state.zonas.getOrElse(2) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.411f, y = h * 0.125f)
            )

            // ZONA 3 (Rear Top Fan Inside Trailer)
            ZoneIndicator(
                label = "ZONA 3",
                status = state.zonas.getOrElse(2) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.885f, y = h * 0.125f)
            )

            // Roof Fans (ZONA 6, ZONA 5, ZONA 4) with Trapezoid Badges
            RoofZoneIndicator(
                label = "ZONA 6",
                status = state.zonas.getOrElse(5) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.531f, y = h * 0.024f)
            )
            RoofZoneIndicator(
                label = "ZONA 5",
                status = state.zonas.getOrElse(4) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.648f, y = h * 0.024f)
            )
            RoofZoneIndicator(
                label = "ZONA 4",
                status = state.zonas.getOrElse(3) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.765f, y = h * 0.024f)
            )

            // Status Header Row & Chart Box
            Column(
                modifier = Modifier
                    .offset(x = w * 0.408f, y = h * 0.212f)
                    .width(w * 0.581f)
            ) {
                // Header Status Display Bar (TEMP / HUMEDAD)
                SensorDisplayHeader(state = state)

                Spacer(modifier = Modifier.height(h * 0.008f))

                // Chart Box with Vertical Dual Scale
                Box(
                    modifier = Modifier
                        .height(h * 0.438f)
                        .fillMaxWidth()
                        .background(Color(0xFF5C6274), RoundedCornerShape(1.dp))
                        .border(1.dp, Color(0xFF3B4050), RoundedCornerShape(1.dp))
                        .padding(2.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Chart Lines
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            HmiLinesChart(state.history)
                        }

                        // Right Vertical Dual Scale Labels (100 50 ... 0 0)
                        DualScaleLabels()
                    }
                }
            }

            // Battery Voltage Box ("0.0 VDC") under rear chassis
            Box(
                modifier = Modifier
                    .offset(x = w * 0.843f, y = h * 0.810f)
                    .width(w * 0.092f)
                    .height(h * 0.077f)
                    .background(Color(0xFFB71C1C), RoundedCornerShape(2.dp))
                    .border(0.5.dp, Color(0xFF444444), RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${state.tension?.let { String.format(Locale.US, "%.1f", it) } ?: "0.0"} VDC",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private val TrapezoidShape = GenericShape { size, _ ->
    val inset = size.width * 0.15f
    moveTo(inset, 0f)
    lineTo(size.width - inset, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

@Composable
private fun RoofZoneIndicator(label: String, status: ZoneStatus, modifier: Modifier = Modifier) {
    val iconRes = when (status) {
        ZoneStatus.ON -> Res.drawable.ic_ventilador_on
        ZoneStatus.OFF -> Res.drawable.ic_ventilador_off
        ZoneStatus.FAIL -> Res.drawable.ic_ventilador_out
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(14.dp)
                .clip(TrapezoidShape)
                .background(Color(0xFF808896)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color.Black,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(1.dp))
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(width = 44.dp, height = 14.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun FlameIndicator(active: Boolean, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.size(width = 28.dp, height = 36.dp)
    ) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.5f, 0f)
            cubicTo(w * 0.7f, h * 0.25f, w * 0.95f, h * 0.45f, w * 0.95f, h * 0.70f)
            cubicTo(w * 0.95f, h * 0.92f, w * 0.75f, h, w * 0.5f, h)
            cubicTo(w * 0.25f, h, 0f, h * 0.92f, 0f, h * 0.70f)
            cubicTo(0f, h * 0.45f, w * 0.3f, h * 0.25f, w * 0.5f, 0f)
            close()
        }

        val innerPath = Path().apply {
            moveTo(w * 0.5f, h * 0.3f)
            cubicTo(w * 0.65f, h * 0.45f, w * 0.8f, h * 0.6f, w * 0.8f, h * 0.75f)
            cubicTo(w * 0.8f, h * 0.9f, w * 0.65f, h * 0.95f, w * 0.5f, h * 0.95f)
            cubicTo(w * 0.35f, h * 0.95f, w * 0.2f, h * 0.9f, w * 0.2f, h * 0.75f)
            cubicTo(w * 0.2f, h * 0.6f, w * 0.35f, h * 0.45f, w * 0.5f, h * 0.3f)
            close()
        }

        val alpha = if (active) 1.0f else 0.4f

        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFF3D00).copy(alpha = alpha),
                    Color(0xFFFF9100).copy(alpha = alpha),
                    Color(0xFFFFD600).copy(alpha = alpha)
                )
            )
        )

        drawPath(
            path = innerPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFEA00).copy(alpha = alpha),
                    Color(0xFFFFFFFF).copy(alpha = alpha)
                )
            )
        )
    }
}

@Composable
private fun SensorDisplayHeader(state: HmiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFDCDCDC), RoundedCornerShape(1.dp))
            .border(1.dp, Color(0xFF444444), RoundedCornerShape(1.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TEMP:",
            color = Color.Black,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )

        // Temp 1 Red Box
        Box(
            modifier = Modifier
                .background(Color(0xFFB71C1C), RoundedCornerShape(2.dp))
                .border(0.5.dp, Color(0xFF444444), RoundedCornerShape(2.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${state.temperatura1?.let { String.format(Locale.US, "%.1f", it) } ?: "0.0"} °C",
                color = Color.Black,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Temp 2 Yellow Box
        Box(
            modifier = Modifier
                .background(Color(0xFFD4E157), RoundedCornerShape(2.dp))
                .border(0.5.dp, Color(0xFF444444), RoundedCornerShape(2.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${state.temperatura2?.let { String.format(Locale.US, "%.1f", it) } ?: "0.0"} °C",
                color = Color.Black,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "HUMEDAD:",
            color = Color.Black,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )

        // Humidity Blue Box
        Box(
            modifier = Modifier
                .background(Color(0xFF0D47A1), RoundedCornerShape(2.dp))
                .border(0.5.dp, Color(0xFF444444), RoundedCornerShape(2.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${state.humedad?.toInt() ?: 0} %",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DualScaleLabels() {
    val scaleValues = listOf(
        100 to 50,
        90 to 45,
        80 to 40,
        70 to 35,
        60 to 30,
        50 to 25,
        40 to 20,
        30 to 15,
        20 to 10,
        10 to 5,
        0 to 0
    )
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(start = 2.dp, end = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End
    ) {
        scaleValues.forEach { (hum, temp) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$hum",
                    color = Color(0xFF1565C0),
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$temp",
                    color = Color(0xFF222222),
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HmiLinesChart(history: List<HmiHistoryPoint>) {
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
        ZoneStatus.ON -> Res.drawable.ic_ventilador_on
        ZoneStatus.OFF -> Res.drawable.ic_ventilador_off
        ZoneStatus.FAIL -> Res.drawable.ic_ventilador_out
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(label, color = Color.Black, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(width = 44.dp, height = 14.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun IrrigatorIndicator(status: ZoneStatus, modifier: Modifier = Modifier) {
    val iconRes = when (status) {
        ZoneStatus.ON -> Res.drawable.ic_aspersor_on
        ZoneStatus.OFF -> Res.drawable.ic_aspersor_off
        ZoneStatus.FAIL -> Res.drawable.ic_aspersor_out
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(width = 44.dp, height = 28.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun AlarmsBanner(alarmas: List<String>) {
    Surface(
        color = Color(0xFFD32F2F),
        shape = RoundedCornerShape(4.dp),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Text(
                text = "ALARMAS: ${alarmas.joinToString(", ")}",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
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

@Preview(widthDp = 895, heightDp = 415, showBackground = true)
@Composable
fun HmiScreenPreview() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CamionColors.panelBackground
    ) {
        HmiScreen(
            state = HmiState(
                idVehiculo = "AAA000",
                conectado = true,
                temperatura1 = 24.5,
                temperatura2 = 23.8,
                humedad = 65.0,
                tension = 24.2,
                zonas = listOf(
                    ZoneStatus.ON,
                    ZoneStatus.ON,
                    ZoneStatus.ON,
                    ZoneStatus.ON,
                    ZoneStatus.ON,
                    ZoneStatus.ON
                ),
                estadoIrrigador = ZoneStatus.ON,
                damperActivo = true,
                calefaccionActiva = true,
                history = listOf(
                    HmiHistoryPoint(20.0, 22.0, 50.0, 1000L),
                    HmiHistoryPoint(22.0, 23.0, 55.0, 2000L),
                    HmiHistoryPoint(24.5, 23.8, 65.0, 3000L)
                )
            )
        )
    }
}


