package com.pronavicola.camion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.pronavicola.camion.presentation.CamionApp
import com.pronavicola.camion.presentation.theme.CamionTheme
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val tag = "MainActivity"

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            Log.i(tag, "GPS location permission granted. Initializing location service.")
            val app = application as CamionApplication
            lifecycleScope.launch {
                app.appContext.gpsLocationTracker.startTracking()
            }
        } else {
            Log.w(tag, "Location permission denied by user. GPS tracking will be disabled.")
            Toast.makeText(
                this,
                "El permiso de ubicación es necesario para el seguimiento GPS del furgón.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        hideSystemBars()

        checkAndRequestLocationPermissions()

        val app = application as CamionApplication
        setContent {
            CamionTheme {
                CamionApp(appContext = app.appContext)
            }
        }
    }

    private fun checkAndRequestLocationPermissions() {
        val hasFine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine) {
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            val app = application as CamionApplication
            lifecycleScope.launch {
                app.appContext.gpsLocationTracker.startTracking()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}
