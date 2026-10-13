package com.pronavicola.camion.service

import android.content.Context
import com.pronavicola.camion.data.local.dao.TrazasUbicacionDao
import kotlinx.coroutines.CoroutineScope

actual fun createGpsLocationTracker(
    platformContext: Any?,
    trazasUbicacionDao: TrazasUbicacionDao,
    deviceIdProvider: suspend () -> String,
    scope: CoroutineScope,
): GpsLocationTracker {
    val context = platformContext as? Context ?: error("Context required for Android GpsLocationTracker")
    return AndroidGpsLocationTracker(context, trazasUbicacionDao, deviceIdProvider, scope)
}
