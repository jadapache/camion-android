package com.pronavicola.camion.service

import com.pronavicola.camion.data.local.dao.TrazasUbicacionDao
import kotlinx.coroutines.CoroutineScope

actual fun createGpsLocationTracker(
    platformContext: Any?,
    trazasUbicacionDao: TrazasUbicacionDao,
    deviceIdProvider: suspend () -> String,
    scope: CoroutineScope,
): GpsLocationTracker {
    return DesktopGpsLocationTracker()
}
