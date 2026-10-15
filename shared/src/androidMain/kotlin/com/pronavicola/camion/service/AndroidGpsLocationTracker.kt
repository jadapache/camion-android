package com.pronavicola.camion.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.pronavicola.camion.data.local.dao.TrazasUbicacionDao
import com.pronavicola.camion.domain.model.GpsLocation
import com.pronavicola.camion.domain.repository.AppConfigRepository
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AndroidGpsLocationTracker(
    private val context: Context,
    private val trazasUbicacionDao: TrazasUbicacionDao,
    private val appConfigRepository: AppConfigRepository,
    private val scope: CoroutineScope
) : GpsLocationTracker {

    private val tag = "AndroidGpsLocationTracker"
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null
    private var isTracking = false

    companion object {
        const val MIN_UPDATE_INTERVAL_MS = 120_000L // 120 seconds (2 minutes)
        const val MIN_DISPLACEMENT_METERS = 100f
    }

    @SuppressLint("MissingPermission")
    override suspend fun startTracking() {
        if (!hasLocationPermission()) {
            Log.w(tag, "ACCESS_FINE_LOCATION permission not granted. GPS tracking disabled.")
            return
        }

        if (isTracking) {
            Log.d(tag, "GPS tracking is already active.")
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            MIN_UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateDistanceMeters(MIN_DISPLACEMENT_METERS)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    scope.launch(Dispatchers.IO) {
                        handleLocationUpdate(location)
                    }
                }
            }
        }

        try {
            fusedClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            isTracking = true
            Log.i(tag, "GPS tracking started (interval: ${MIN_UPDATE_INTERVAL_MS}ms, min displacement: ${MIN_DISPLACEMENT_METERS}m)")
        } catch (e: SecurityException) {
            Log.e(tag, "SecurityException starting GPS tracking: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(tag, "Error starting GPS tracking: ${e.message}", e)
        }
    }

    override suspend fun stopTracking() {
        locationCallback?.let {
            fusedClient.removeLocationUpdates(it)
            locationCallback = null
        }
        isTracking = false
        Log.i(tag, "GPS tracking stopped")
    }

    private suspend fun handleLocationUpdate(location: Location) {
        val gpsLocation = GpsLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = if (location.hasAltitude()) location.altitude else null,
            accuracy = if (location.hasAccuracy()) location.accuracy else null,
            speed = if (location.hasSpeed()) location.speed else null,
            timestamp = location.time
        )

        if (!gpsLocation.isValid()) {
            Log.w(tag, "Discarding invalid GPS coordinates: lat=${gpsLocation.latitude}, lon=${gpsLocation.longitude}")
            return
        }

        val deviceId = appConfigRepository.getDeviceId() ?: "UNKNOWN"
        val entity = gpsLocation.toTrazasUbicacionEntity(deviceId)

        try {
            trazasUbicacionDao.insert(entity)
            Log.i(tag, "GPS location stored: lat=${gpsLocation.latitude}, lon=${gpsLocation.longitude}, accuracy=${gpsLocation.accuracy}m, speed=${gpsLocation.speed}m/s")
        } catch (e: Exception) {
            Log.e(tag, "Failed to persist GPS location to database: ${e.message}", e)
        }
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}

actual fun createGpsLocationTracker(
    platformContext: Any?,
    trazasUbicacionDao: TrazasUbicacionDao,
    appConfigRepository: AppConfigRepository,
    scope: CoroutineScope
): GpsLocationTracker {
    val context = platformContext as? Context ?: error("Android Context required for GpsLocationTracker")
    return AndroidGpsLocationTracker(context, trazasUbicacionDao, appConfigRepository, scope)
}
