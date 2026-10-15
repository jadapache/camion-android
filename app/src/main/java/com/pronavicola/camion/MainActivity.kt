package com.pronavicola.camion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import com.pronavicola.camion.service.MqttForegroundService
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val tag = "MainActivity"

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        Log.i(tag, "Permissions result: FINE=$fineGranted, COARSE=$coarseGranted")
        if (fineGranted || coarseGranted) {
            onLocationPermissionGranted()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        hideSystemBars()
        checkAndRequestPermissions()

        val app = application as CamionApplication
        setContent {
            CamionTheme {
                CamionApp(appContext = app.appContext)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasLocationPermission()) {
            onLocationPermissionGranted()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun checkAndRequestPermissions() {
        val needed = mutableListOf<String>()

        if (!hasLocationPermission()) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationsGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notificationsGranted) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (needed.isNotEmpty()) {
            Log.i(tag, "Requesting permissions: $needed")
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            onLocationPermissionGranted()
        }
    }

    private fun onLocationPermissionGranted() {
        val app = application as? CamionApplication ?: return
        if (app.isInitialized) {
            app.appContext.appScope.launch {
                try {
                    app.appContext.gpsLocationTracker.startTracking()
                    Log.i(tag, "GPS location tracker started following permission grant")
                } catch (e: Exception) {
                    Log.e(tag, "Error starting GPS tracker: ${e.message}")
                }
            }
        }
        try {
            MqttForegroundService.startService(this)
        } catch (e: Exception) {
            Log.e(tag, "Error restarting MqttForegroundService: ${e.message}")
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}
