package com.pronavicola.camion.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.pronavicola.camion.data.local.dao.UbicacionDao
import com.pronavicola.camion.data.local.dao.VehiculoDao
import com.pronavicola.camion.domain.model.GpsLocation
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class AndroidGpsLocationTracker(
    private val context: Context,
    private val ubicacionDao: UbicacionDao,
    private val vehiculoDao: VehiculoDao,
    private val deviceIdProvider: suspend () -> String,
    private val scope: CoroutineScope,
) : GpsLocationTracker {

    private val tag = "AndroidGpsLocationTracker"
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _currentLocation = MutableStateFlow<GpsLocation?>(null)
    override val currentLocation: StateFlow<GpsLocation?> = _currentLocation.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    override val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private var heartbeatJob: Job? = null
    private var lastPersistTimeMs: Long = 0L

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
        const val MIN_DISPLACEMENT_METERS = 0f // 0 meters - ensures updates even when stationary/parked
    }

    @SuppressLint("MissingPermission")
    override suspend fun startTracking() = withContext(Dispatchers.Main) {
        if (_isTracking.value) return@withContext

        if (locationManager == null) {
            Log.e(tag, "LocationManager not available")
            return@withContext
        }

        try {
            val enabledProviders = locationManager.getProviders(true)
            if (enabledProviders.isEmpty()) {
                Log.w(tag, "No enabled location providers found")
            } else {
                Log.i(tag, "Enabled location providers: $enabledProviders")
            }

            var registeredAny = false
            for (provider in enabledProviders) {
                try {
                    locationManager.requestLocationUpdates(
                        provider,
                        MIN_UPDATE_INTERVAL_MS,
                        MIN_DISPLACEMENT_METERS,
                        locationListener,
                        Looper.getMainLooper()
                    )
                    registeredAny = true
                    Log.d(tag, "Requested location updates from provider: $provider")
                } catch (e: Exception) {
                    Log.w(tag, "Failed to request location updates from $provider: ${e.message}")
                }
            }

            if (!registeredAny) {
                val hasGps = try { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false }
                val hasNetwork = try { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { false }
                if (hasGps) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_UPDATE_INTERVAL_MS,
                        MIN_DISPLACEMENT_METERS,
                        locationListener,
                        Looper.getMainLooper()
                    )
                    registeredAny = true
                }
                if (hasNetwork) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_UPDATE_INTERVAL_MS,
                        MIN_DISPLACEMENT_METERS,
                        locationListener,
                        Looper.getMainLooper()
                    )
                    registeredAny = true
                }
            }

            // Get last known location immediately across all providers
            var bestLast: Location? = null
            for (provider in locationManager.allProviders) {
                try {
                    val last = locationManager.getLastKnownLocation(provider)
                    if (last != null && (bestLast == null || last.time > bestLast.time)) {
                        bestLast = last
                    }
                } catch (_: Exception) {}
            }
            bestLast?.let { handleLocationUpdate(it) }

            _isTracking.value = true
            Log.i(tag, "GPS tracking started successfully (registeredAny=$registeredAny)")

            // Periodic heartbeat to refresh stationary location coordinates even when stationary indoors
            heartbeatJob?.cancel()
            heartbeatJob = scope.launch(Dispatchers.IO) {
                while (isActive) {
                    delay(MIN_UPDATE_INTERVAL_MS)
                    if (!_isTracking.value) break

                    val now = System.currentTimeMillis()
                    if (now - lastPersistTimeMs >= (MIN_UPDATE_INTERVAL_MS - 2000L)) {
                        var candidate: Location? = null
                        try {
                            for (p in locationManager.allProviders) {
                                val last = locationManager.getLastKnownLocation(p)
                                if (last != null && (candidate == null || last.time > candidate.time)) {
                                    candidate = last
                                }
                            }
                        } catch (_: Exception) {}

                        if (candidate != null) {
                            handleLocationUpdate(candidate)
                        } else {
                            val curr = _currentLocation.value
                            if (curr != null) {
                                persistGpsLocation(curr.copy(marcaTiempo = now))
                            }
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            _isTracking.value = false
            Log.e(tag, "Missing location permissions: ${e.message}")
        } catch (e: Exception) {
            _isTracking.value = false
            Log.e(tag, "Error starting location tracking: ${e.message}")
        }
    }

    override suspend fun stopTracking() = withContext(Dispatchers.Main) {
        if (!_isTracking.value) return@withContext
        try {
            heartbeatJob?.cancel()
            heartbeatJob = null
            locationManager?.removeUpdates(locationListener)
            _isTracking.value = false
            Log.i(tag, "GPS tracking stopped")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping location tracking: ${e.message}")
        }
    }

    private fun handleLocationUpdate(location: Location) {
        val now = System.currentTimeMillis()
        val gpsLocation = GpsLocation(
            latitud = location.latitude,
            longitud = location.longitude,
            altitud = if (location.hasAltitude()) location.altitude else null,
            precision = if (location.hasAccuracy()) location.accuracy else null,
            velocidad = if (location.hasSpeed()) location.speed else null,
            marcaTiempo = now,
        )

        _currentLocation.value = gpsLocation
        Log.d(tag, "New location: lat=${gpsLocation.latitud}, lng=${gpsLocation.longitud}, acc=${gpsLocation.precision}")

        persistGpsLocation(gpsLocation)
    }

    private fun persistGpsLocation(gpsLocation: GpsLocation) {
        lastPersistTimeMs = System.currentTimeMillis()
        scope.launch(Dispatchers.IO) {
            try {
                val deviceId = deviceIdProvider()
                val idVehiculo = vehiculoDao.getPredeterminado()?.idVehiculo ?: 1
                val idUbicacion = ubicacionDao.getNextIdUbicacion(idVehiculo)
                val entity = gpsLocation.toUbicacionEntity(
                    idVehiculo = idVehiculo,
                    deviceId = deviceId,
                    idUbicacion = idUbicacion,
                )
                ubicacionDao.insert(entity)
            } catch (e: Exception) {
                Log.e(tag, "Failed to persist GPS location to database: ${e.message}")
            }
        }
    }
}
