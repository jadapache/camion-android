package com.pronavicola.camion.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.pronavicola.camion.data.local.dao.TrazasUbicacionDao
import com.pronavicola.camion.domain.model.GpsLocation
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AndroidGpsLocationTracker(
    private val context: Context,
    private val trazasUbicacionDao: TrazasUbicacionDao,
    private val deviceIdProvider: suspend () -> String,
    private val scope: CoroutineScope,
) : GpsLocationTracker {

    private val tag = "AndroidGpsLocationTracker"
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _currentLocation = MutableStateFlow<GpsLocation?>(null)
    override val currentLocation: StateFlow<GpsLocation?> = _currentLocation.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    override val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            handleLocationUpdate(location)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {
            Log.d(tag, "Location provider enabled: $provider")
        }
        override fun onProviderDisabled(provider: String) {
            Log.w(tag, "Location provider disabled: $provider")
        }
    }

    companion object {
        const val MIN_UPDATE_INTERVAL_MS = 15_000L // 15 seconds
        const val MIN_DISPLACEMENT_METERS = 5f // 5 meters
    }

    @SuppressLint("MissingPermission")
    override suspend fun startTracking() = withContext(Dispatchers.Main) {
        if (_isTracking.value) return@withContext

        if (locationManager == null) {
            Log.e(tag, "LocationManager not available")
            return@withContext
        }

        try {
            val hasGps = try { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false }
            val hasNetwork = try { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { false }

            if (!hasGps && !hasNetwork) {
                Log.w(tag, "No location providers enabled")
            }

            if (hasGps) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_UPDATE_INTERVAL_MS,
                    MIN_DISPLACEMENT_METERS,
                    locationListener,
                    Looper.getMainLooper()
                )
            }

            if (hasNetwork) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_UPDATE_INTERVAL_MS,
                    MIN_DISPLACEMENT_METERS,
                    locationListener,
                    Looper.getMainLooper()
                )
            }

            // Get last known location immediately
            val lastGps = if (hasGps) locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
            val lastNetwork = if (hasNetwork) locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null
            val bestLast = lastGps ?: lastNetwork
            bestLast?.let { handleLocationUpdate(it) }

            _isTracking.value = true
            Log.i(tag, "GPS tracking started successfully")
        } catch (e: SecurityException) {
            Log.e(tag, "Missing location permissions: ${e.message}")
        } catch (e: Exception) {
            Log.e(tag, "Error starting location tracking: ${e.message}")
        }
    }

    override suspend fun stopTracking() = withContext(Dispatchers.Main) {
        if (!_isTracking.value) return@withContext
        try {
            locationManager?.removeUpdates(locationListener)
            _isTracking.value = false
            Log.i(tag, "GPS tracking stopped")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping location tracking: ${e.message}")
        }
    }

    private fun handleLocationUpdate(location: Location) {
        val gpsLocation = GpsLocation(
            latitud = location.latitude,
            longitud = location.longitude,
            altitud = if (location.hasAltitude()) location.altitude else null,
            precision = if (location.hasAccuracy()) location.accuracy else null,
            velocidad = if (location.hasSpeed()) location.speed else null,
            marcaTiempo = if (location.time > 0) location.time else System.currentTimeMillis()
        )

        _currentLocation.value = gpsLocation
        Log.d(tag, "New location: lat=${gpsLocation.latitud}, lng=${gpsLocation.longitud}, acc=${gpsLocation.precision}")

        scope.launch(Dispatchers.IO) {
            try {
                val deviceId = deviceIdProvider()
                val entity = gpsLocation.toTrazasUbicacionEntity(deviceId)
                trazasUbicacionDao.insert(entity)
            } catch (e: Exception) {
                Log.e(tag, "Failed to persist GPS trace to database: ${e.message}")
            }
        }
    }
}
