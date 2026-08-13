package com.pronavicola.camion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pronavicola.camion.ui.CamionApp
import com.pronavicola.camion.ui.theme.CamionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CamionApplication
        setContent {
            CamionTheme {
                CamionApp(app = app)
            }
        }
    }
}
