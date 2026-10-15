package com.pronavicola.camion.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pronavicola.camion.data.local.createFurgonesDatabase
import com.pronavicola.camion.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HistoricalDataCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val tag = "HistoricalDataCleanupWorker"

    companion object {
        const val WORK_NAME = "HistoricalDataCleanup"
        const val RETENTION_DAYS = 60L
        const val BATCH_SIZE = 1000

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val cleanupRequest = PeriodicWorkRequestBuilder<HistoricalDataCleanupWorker>(
                1, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(calculateDelayUntil3AM(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
            )
            Log.i("HistoricalDataCleanupWorker", "Daily cleanup scheduled for 03:00 AM (retention: 60 days)")
        }

        fun calculateDelayUntil3AM(): Long {
            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 3)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            if (calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            return calendar.timeInMillis - now
        }
    }

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        Log.i(tag, "Starting historical data cleanup worker (retention: $RETENTION_DAYS days)")

        return try {
            val database = createFurgonesDatabase(applicationContext)
            val variableRegistroDao = database.variableRegistroDao()
            val trazasUbicacionDao = database.trazasUbicacionDao()

            val cutoffEpoch = System.currentTimeMillis() - (RETENTION_DAYS * 24 * 3600 * 1000L)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val cutoffDateStr = sdf.format(Date(cutoffEpoch))

            var totalTelemetryDeleted = 0
            var batchDeleted: Int
            do {
                batchDeleted = variableRegistroDao.deleteOlderThanBatch(cutoffDateStr, BATCH_SIZE)
                totalTelemetryDeleted += batchDeleted
                if (batchDeleted > 0) {
                    Log.d(tag, "Deleted $batchDeleted telemetry records batch")
                }
            } while (batchDeleted > 0)

            val gpsDeleted = trazasUbicacionDao.deleteOlderThan(cutoffEpoch)
            val executionTimeMs = System.currentTimeMillis() - startTime

            Log.i(
                tag,
                "Historical data cleanup complete in ${executionTimeMs}ms: deleted $totalTelemetryDeleted telemetry records, $gpsDeleted GPS traces older than $cutoffDateStr"
            )

            Result.success()
        } catch (e: Exception) {
            Log.e(tag, "Historical data cleanup failed: ${e.message}", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
