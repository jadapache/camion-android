package com.pronavicola.camion.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pronavicola.camion.CamionApplication
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class HistoricalDataCleanupWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val app = applicationContext as? CamionApplication
            val cleanupUseCase = app?.appContext?.cleanupHistoricalDataUseCase ?: return@withContext Result.failure()
            val result = cleanupUseCase(RETENTION_DAYS.toLong())

            Log.i(
                TAG,
                "Cleanup completed in ${result.durationMs}ms: deleted ${result.deletedTelemetry} telemetry records and ${result.deletedGps} GPS records older than $RETENTION_DAYS days."
            )
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "HistoricalDataCleanupWorker"
        const val WORK_NAME = "HistoricalDataCleanup"
        const val RETENTION_DAYS = 60

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val cleanupRequest = PeriodicWorkRequestBuilder<HistoricalDataCleanupWorker>(
                24, TimeUnit.HOURS,
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
            )
            Log.i(TAG, "Scheduled daily periodic cleanup worker ($WORK_NAME)")
        }
    }
}
