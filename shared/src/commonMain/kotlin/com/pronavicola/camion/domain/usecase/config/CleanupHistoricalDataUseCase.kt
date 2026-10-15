package com.pronavicola.camion.domain.usecase.config

import com.pronavicola.camion.data.local.dao.UbicacionDao
import com.pronavicola.camion.data.local.dao.VariableRegistroDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CleanupResult(
    val deletedTelemetry: Int,
    val deletedGps: Int,
    val durationMs: Long,
)

class CleanupHistoricalDataUseCase(
    private val variableRegistroDao: VariableRegistroDao,
    private val ubicacionDao: UbicacionDao,
) {
    suspend operator fun invoke(retentionDays: Long = 60L): CleanupResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val cutoff = startTime - (retentionDays * 24L * 60L * 60L * 1000L)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val cutoffStr = dateFormat.format(Date(cutoff))

        var totalDeletedTelemetry = 0
        do {
            val deleted = variableRegistroDao.deleteOlderThanBatch(cutoffStr, 1000)
            totalDeletedTelemetry += deleted
        } while (deleted == 1000)

        val deletedGps = ubicacionDao.deleteOlderThan(cutoff)
        val durationMs = System.currentTimeMillis() - startTime

        CleanupResult(
            deletedTelemetry = totalDeletedTelemetry,
            deletedGps = deletedGps,
            durationMs = durationMs
        )
    }
}
