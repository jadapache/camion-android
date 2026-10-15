package com.pronavicola.camion.service

import com.pronavicola.camion.data.local.dao.TrazasUbicacionDao
import com.pronavicola.camion.domain.model.GpsLocation
import com.pronavicola.camion.domain.repository.AppConfigRepository
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class DesktopGpsLocationTracker(
    private val trazasUbicacionDao: TrazasUbicacionDao,
    private val appConfigRepository: AppConfigRepository,
    private val scope: CoroutineScope
) : GpsLocationTracker {

    private val tag = "DesktopGpsLocationTracker"
    private var trackingJob: Job? = null

    override suspend fun startTracking() {
        Log.i(tag, "GPS tracking on Desktop - starting Santiago simulation")
        trackingJob?.cancel()
        trackingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val simulatedLocation = GpsLocation(
                    latitude = -33.4489 + (Random.nextDouble() - 0.5) * 0.01,
                    longitude = -70.6693 + (Random.nextDouble() - 0.5) * 0.01,
                    altitude = 570.0 + Random.nextDouble() * 10.0,
                    accuracy = 10f + (Random.nextFloat() * 5f),
                    speed = (Random.nextFloat() * 20f),
                    timestamp = System.currentTimeMillis()
                )

                val deviceId = appConfigRepository.getDeviceId() ?: "DESKTOP_SIM"
                try {
                    trazasUbicacionDao.insert(simulatedLocation.toTrazasUbicacionEntity(deviceId))
                    Log.d(tag, "Simulated GPS trace persisted for device $deviceId: lat=${simulatedLocation.latitude}, lon=${simulatedLocation.longitude}")
                } catch (e: Exception) {
                    Log.e(tag, "Failed to persist simulated GPS trace: ${e.message}", e)
                }

                delay(120_000L) // 120 seconds
            }
        }
    }

    override suspend fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        Log.i(tag, "Desktop simulated GPS tracking stopped")
    }
}

actual fun createGpsLocationTracker(
    platformContext: Any?,
    trazasUbicacionDao: TrazasUbicacionDao,
    appConfigRepository: AppConfigRepository,
    scope: CoroutineScope
): GpsLocationTracker {
    return DesktopGpsLocationTracker(trazasUbicacionDao, appConfigRepository, scope)
}
