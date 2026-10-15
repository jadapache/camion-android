package com.pronavicola.camion.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pronavicola.camion.CamionAppContext
import com.pronavicola.camion.presentation.CamionApp
import com.pronavicola.camion.presentation.theme.CamionTheme

fun main() = application {
    val appContext = remember {
        CamionAppContext().apply {
            startBackgroundServices()
        }
    }

    var isFullscreen by remember { mutableStateOf(true) }
    val windowState = rememberWindowState(
        placement = if (isFullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating,
        size = DpSize(1280.dp, 800.dp),
        position = WindowPosition(Alignment.Center),
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Camion HMI",
        state = windowState,
        onKeyEvent = { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.F11) {
                isFullscreen = !isFullscreen
                windowState.placement = if (isFullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating
                true
            } else {
                false
            }
        },
    ) {
        CamionTheme {
            CamionApp(appContext = appContext)
        }
    }
}
