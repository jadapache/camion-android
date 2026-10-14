package com.pronavicola.camion.service

import com.pronavicola.camion.data.local.dao.UbicacionDao
import com.pronavicola.camion.data.local.dao.VehiculoDao
import com.pronavicola.camion.domain.model.GpsLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

interface GpsLocationTracker {
    val currentLocation: StateFlow<GpsLocation?>
    val isTracking: StateFlow<Boolean>

    suspend fun startTracking()
    suspend fun stopTracking()
}

expect fun createGpsLocationTracker(
    platformContext: Any?,
    ubicacionDao: UbicacionDao,
    vehiculoDao: VehiculoDao,
    deviceIdProvider: suspend () -> String,
    scope: CoroutineScope,
): GpsLocationTracker
