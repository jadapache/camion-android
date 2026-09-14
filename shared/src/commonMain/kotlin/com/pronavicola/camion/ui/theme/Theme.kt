package com.pronavicola.camion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CamionBlue = Color(0xFF2E5E8C)
private val CamionBlueDark = Color(0xFF1E4468)
private val CamionPanel = Color(0xFF708FA6)
private val CamionAccent = Color(0xFF008700)

private val LightColors = lightColorScheme(
    primary = CamionBlue,
    onPrimary = Color.White,
    primaryContainer = CamionBlueDark,
    secondary = CamionAccent,
    background = Color(0xFFF2F4F7),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
)

@Composable
fun CamionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}

object CamionColors {
    val panelBackground = CamionPanel
    val headerBackground = CamionBlue
    val editableRow = Color.White
    val readOnlyRow = Color(0xFFE0E0E0)
    val connected = Color(0xFF008700)
    val disconnected = Color(0xFFB00020)
    val truckBody = Color(0xFFD9D9D9)
    val truckTire = Color(0xFF333333)
    val zoneActive = Color(0xFF4CAF50)
    val zoneInactive = Color(0xFF757575)
    val sensorBox = Color(0xFF2E5E8C)
}
