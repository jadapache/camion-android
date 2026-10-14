package com.pronavicola.camion.service

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
    return DesktopGpsLocationTracker()
}
