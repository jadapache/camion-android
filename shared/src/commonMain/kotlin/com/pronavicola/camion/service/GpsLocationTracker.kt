package com.pronavicola.camion.service

import com.pronavicola.camion.data.local.dao.TrazasUbicacionDao
import com.pronavicola.camion.domain.repository.AppConfigRepository
import kotlinx.coroutines.CoroutineScope

interface GpsLocationTracker {
    suspend fun startTracking()
    suspend fun stopTracking()
}

expect fun createGpsLocationTracker(
    platformContext: Any?,
    trazasUbicacionDao: TrazasUbicacionDao,
    appConfigRepository: AppConfigRepository,
    scope: CoroutineScope
): GpsLocationTracker
