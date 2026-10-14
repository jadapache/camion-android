package com.pronavicola.camion.service

import android.content.Context
import com.pronavicola.camion.data.local.dao.UbicacionDao
import com.pronavicola.camion.data.local.dao.VehiculoDao
import kotlinx.coroutines.CoroutineScope

actual fun createGpsLocationTracker(
    platformContext: Any?,
    ubicacionDao: UbicacionDao,
    vehiculoDao: VehiculoDao,
    deviceIdProvider: suspend () -> String,
    scope: CoroutineScope,
): GpsLocationTracker {
    val context = platformContext as? Context ?: error("Context required for Android GpsLocationTracker")
    return AndroidGpsLocationTracker(context, ubicacionDao, vehiculoDao, deviceIdProvider, scope)
}
