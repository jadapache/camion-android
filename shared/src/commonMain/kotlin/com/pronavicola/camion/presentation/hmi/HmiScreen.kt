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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import androidx.compose.ui.tooling.preview.Preview
import java.util.Locale

@Composable
fun HmiScreen(
    state: HmiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CamionColors.panelBackground),
        contentAlignment = Alignment.Center
    ) {
        // Main HMI Area with fixed 730:420 aspect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            TruckHmiLayout(state = state)
        }

        // Connection Badge & Alarms at the bottom left
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
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
    BoxWithConstraints(
        modifier = Modifier
            .aspectRatio(730f / 420f)
            .fillMaxWidth()
    ) {
        val w = this.maxWidth
        val h = this.maxHeight

        Box(modifier = Modifier.fillMaxSize()) {
            // SVG Truck Image
            Image(
                painter = painterResource(Res.drawable.ic_truck_silhouette),
                contentDescription = "Truck Silhouette",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            // License Plate ("AAA000") on door
            Box(
                modifier = Modifier
                    .offset(x = w * 0.134f, y = h * 0.576f)
                    .width(w * 0.098f)
                    .height(h * 0.068f)
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
                val x1 = size.width * (252f / 730f)
                val y1 = size.height * (105f / 420f)
                val x2 = size.width * (286f / 730f)
                val y2 = size.height * (128f / 420f)

                val strokeW = size.height * (8f / 420f)
                val barColor = if (state.damperActivo) Color(0xFF1B5E20) else Color(0xFF616161)

                drawLine(
                    color = barColor,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }

            // Roof Fans (ZONA 6, ZONA 5, ZONA 4) resting directly at the roof line
            RoofZoneIndicator(
                label = "ZONA 6",
                status = state.zonas.getOrElse(5) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.535f, y = h * 0.198f)
            )
            RoofZoneIndicator(
                label = "ZONA 5",
                status = state.zonas.getOrElse(4) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.650f, y = h * 0.198f)
            )
            RoofZoneIndicator(
                label = "ZONA 4",
                status = state.zonas.getOrElse(3) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.762f, y = h * 0.198f)
            )

            // ZONA 3 (Front Top Fan Inside Trailer - lowered below roof)
            ZoneIndicator(
                label = "ZONA 3",
                status = state.zonas.getOrElse(2) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.448f, y = h * 0.255f)
            )

            // ZONA 3 (Rear Top Fan Inside Trailer - lowered below roof)
            ZoneIndicator(
                label = "ZONA 3",
                status = state.zonas.getOrElse(2) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.863f, y = h * 0.255f)
            )

            // ZONA 1 (Front Column Top Fan)
            ZoneIndicator(
                label = "ZONA 1",
                status = state.zonas.getOrElse(0) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.348f, y = h * 0.335f)
            )

            // ZONA 2 (Front Column Middle Fan)
            ZoneIndicator(
                label = "ZONA 2",
                status = state.zonas.getOrElse(1) { ZoneStatus.OFF },
                modifier = Modifier.offset(x = w * 0.348f, y = h * 0.440f)
            )

            // Aspersor / Nebulizador / Irrigator ("BOMBA") - flush against container border
            IrrigatorIndicator(
                status = state.estadoIrrigador,
                modifier = Modifier
                    .offset(x = w * 0.339f, y = h * 0.525f)
                    .width(w * 0.074f)
                    .height(h * 0.155f)
            )

            // Gotas de dispersión de agua cuando el irrigador está activo (ON)
            if (state.estadoIrrigador == ZoneStatus.ON) {
                WaterSprayDispersion(
                    modifier = Modifier
                        .offset(x = w * 0.370f, y = h * 0.518f)
                        .width(w * 0.088f)
                        .height(h * 0.065f)
                )
            }

            // Flame / Fire Icon ("Calor / Calefacción") - separated slightly from irrigator
            FlameIndicator(
                active = state.calefaccionActiva,
                modifier = Modifier.offset(x = w * 0.388f, y = h * 0.590f)
            )

            // Status Header Row & Chart Box - adjusted to stay comfortably within the container
            Column(
                modifier = Modifier
                    .offset(x = w * 0.440f, y = h * 0.338f)
                    .width(w * 0.508f)
                    .height(h * 0.325f)
            ) {
                // Header Status Display Bar (TEMP / HUMEDAD)
                SensorDisplayHeader(state = state)

                Spacer(modifier = Modifier.height(1.dp))

                // Chart Box with Vertical Dual Scale
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF555C6E), RoundedCornerShape(1.dp))
                        .border(1.dp, Color(0xFF3B4050), RoundedCornerShape(1.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Chart Lines
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clipToBounds()) {
                            HmiLinesChart(state.history)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Right Vertical Dual Scale Labels (100 50 ... 0 0)
                        DualScaleLabels()
                    }
                }
            }

            // Battery Voltage Box ("24.2 VDC") on lower front chassis
            Box(
                modifier = Modifier
                    .offset(x = w * 0.347f, y = h * 0.690f)
                    .width(w * 0.082f)
                    .height(h * 0.055f)
                    .background(Color(0xFFB71C1C), RoundedCornerShape(1.dp))
                    .border(0.5.dp, Color(0xFF444444), RoundedCornerShape(1.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${state.tension?.let { String.format(Locale.US, "%.1f", it) } ?: "0.0"} VDC",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.5.sp
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
                .width(46.dp)
                .height(13.dp)
                .clip(TrapezoidShape)
                .background(Color(0xFF737B8B)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color.Black,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(width = 46.dp, height = 15.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun FlameIndicator(active: Boolean, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.size(width = 26.dp, height = 34.dp)
    ) {
        val w = size.width
        val h = size.height
        val alpha = if (active) 1.0f else 0.3f

        val outerFlame = Path().apply {
            moveTo(w * 0.50f, 0f)
            cubicTo(w * 0.58f, h * 0.18f, w * 0.95f, h * 0.40f, w * 0.95f, h * 0.68f)
            cubicTo(w * 0.95f, h * 0.92f, w * 0.75f, h, w * 0.50f, h)
            cubicTo(w * 0.25f, h, w * 0.05f, h * 0.92f, w * 0.05f, h * 0.68f)
            cubicTo(w * 0.05f, h * 0.40f, w * 0.42f, h * 0.18f, w * 0.50f, 0f)
            close()
        }

        val innerFlame = Path().apply {
            moveTo(w * 0.50f, h * 0.32f)
            cubicTo(w * 0.62f, h * 0.48f, w * 0.78f, h * 0.60f, w * 0.78f, h * 0.75f)
            cubicTo(w * 0.78f, h * 0.90f, w * 0.65f, h * 0.96f, w * 0.50f, h * 0.96f)
            cubicTo(w * 0.35f, h * 0.96f, w * 0.22f, h * 0.90f, w * 0.22f, h * 0.75f)
            cubicTo(w * 0.22f, h * 0.60f, w * 0.38f, h * 0.48f, w * 0.50f, h * 0.32f)
            close()
        }

        drawPath(
            path = outerFlame,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFF5722).copy(alpha = alpha),
                    Color(0xFFFF9800).copy(alpha = alpha),
                    Color(0xFFFFC107).copy(alpha = alpha)
                )
            )
        )

        drawPath(
            path = innerFlame,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFEB3B).copy(alpha = alpha),
                    Color(0xFFFFF9C4).copy(alpha = alpha)
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
            .border(1.dp, Color(0xFF555555), RoundedCornerShape(1.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TEMP:",
            color = Color.Black,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )

        // Temp 1 Red Box
        Box(
            modifier = Modifier
                .background(Color(0xFFB71C1C), RoundedCornerShape(2.dp))
                .border(0.5.dp, Color(0xFF444444), RoundedCornerShape(2.dp))
                .padding(horizontal = 6.dp, vertical = 1.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${state.temperatura1?.let { String.format(Locale.US, "%.1f", it) } ?: "0.0"} °C",
                color = Color.Black,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Temp 2 Yellow Box
        Box(
            modifier = Modifier
                .background(Color(0xFFD4E157), RoundedCornerShape(2.dp))
                .border(0.5.dp, Color(0xFF444444), RoundedCornerShape(2.dp))
                .padding(horizontal = 6.dp, vertical = 1.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${state.temperatura2?.let { String.format(Locale.US, "%.1f", it) } ?: "0.0"} °C",
                color = Color.Black,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "HUMEDAD:",
            color = Color.Black,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )

        // Humidity Blue Box
        Box(
            modifier = Modifier
                .background(Color(0xFF0D47A1), RoundedCornerShape(2.dp))
                .border(0.5.dp, Color(0xFF444444), RoundedCornerShape(2.dp))
                .padding(horizontal = 6.dp, vertical = 1.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${state.humedad?.toInt() ?: 0} %",
                color = Color.White,
                fontSize = 8.sp,
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
            .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End
    ) {
        scaleValues.forEach { (hum, temp) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$hum",
                    color = Color(0xFF1E88E5),
                    fontSize = 5.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$temp",
                    color = Color(0xFF1E1E1E),
                    fontSize = 5.5.sp,
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

        // 11 Grid Lines matching the 11 Dual Scale points
        for (i in 0..10) {
            val y = height - (i * height / 10f)
            drawLine(
                color = Color(0xFF6B7285).copy(alpha = 0.45f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 0.5.dp.toPx()
            )
        }

        if (history.size < 2) return@Canvas

        val stepX = width / (maxPoints - 1)

        fun drawSeries(values: List<Double>, maxVal: Double, color: Color) {
            val points = values.mapIndexed { index, value ->
                // Anchor newest data points at the right edge, flowing towards the left
                val x = width - ((values.size - 1 - index) * stepX)
                val normalized = (value / maxVal).coerceIn(0.0, 1.0).toFloat()
                val y = height - (normalized * height)
                Offset(x, y)
            }

            if (points.isEmpty()) return

            val path = Path()
            path.moveTo(points.first().x, points.first().y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cx1 = p0.x + (p1.x - p0.x) / 2f
                val cy1 = p0.y
                val cx2 = p0.x + (p1.x - p0.x) / 2f
                val cy2 = p1.y
                path.cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = 1.8.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Humedad (0..100) - Cyan/Blue
        drawSeries(history.map { it.hum }, 100.0, Color(0xFF00E5FF))
        // Temperatura 2 (0..50) - Yellow
        drawSeries(history.map { it.t2 }, 50.0, Color(0xFFFFEB3B))
        // Temperatura 1 (0..50) - Red/Coral
        drawSeries(history.map { it.t1 }, 50.0, Color(0xFFFF5252))
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
            modifier = Modifier.size(width = 46.dp, height = 15.dp),
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
    Image(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun WaterSprayDispersion(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h * 0.5f

        // Central horizontal stream
        val centerJetPath = Path().apply {
            moveTo(0f, centerY - 1.2.dp.toPx())
            lineTo(w * 0.52f, centerY - 0.7.dp.toPx())
            lineTo(w * 0.52f, centerY + 0.7.dp.toPx())
            lineTo(0f, centerY + 1.2.dp.toPx())
            close()
        }
        drawPath(
            path = centerJetPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF0D47A1),
                    Color(0xFF1565C0),
                    Color(0xFF1E88E5).copy(alpha = 0.5f)
                )
            )
        )

        // Conical spray pattern of dispersing water droplets
        val colCount = 6
        for (col in 1..colCount) {
            val colFraction = col.toFloat() / colCount
            val colX = colFraction * w
            val dropletCount = 2 * col + 1
            val maxSpreadY = colFraction * h * 0.46f

            for (d in 0 until dropletCount) {
                val dFraction = d.toFloat() / (dropletCount - 1) * 2f - 1f
                val dropletY = centerY + (dFraction * maxSpreadY)

                val angleRad = kotlin.math.atan2(dropletY - centerY, colX)
                val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()

                val dropAlpha = (1f - (colFraction * 0.25f)).coerceIn(0.45f, 0.95f)
                val dropColor = when {
                    col <= 2 -> Color(0xFF0D47A1).copy(alpha = dropAlpha)
                    col <= 4 -> Color(0xFF1565C0).copy(alpha = dropAlpha)
                    else -> Color(0xFF1E88E5).copy(alpha = dropAlpha)
                }

                rotate(degrees = angleDeg, pivot = Offset(colX, dropletY)) {
                    val ovalW = (3.8f + col * 0.5f).dp.toPx()
                    val ovalH = (1.5f + col * 0.18f).dp.toPx()
                    drawOval(
                        color = dropColor,
                        topLeft = Offset(colX - ovalW / 2f, dropletY - ovalH / 2f),
                        size = Size(ovalW, ovalH)
                    )
                }
            }
        }
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
        color = Color.White
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
                history = List(45) { i ->
                    val sin1 = kotlin.math.sin(i * 0.4) * 8 + 24.5
                    val sin2 = kotlin.math.cos(i * 0.35) * 6 + 23.8
                    val sin3 = kotlin.math.sin(i * 0.5 + 1.2) * 18 + 65.0
                    HmiHistoryPoint(sin1, sin2, sin3, (i * 1000).toLong())
                }
            )
        )
    }
}



