package com.pronavicola.camion.data.mqtt

import com.pronavicola.camion.data.local.dao.BandejaSalidaMqttDao
import com.pronavicola.camion.data.local.entity.BandejaSalidaMqttEntity
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random

/**
 * Gestor de cola FIFO persistente fuera de línea utilizando la tabla BandejaSalidaMqtt de Room
 * con política de reintentos basada en backoff exponencial y jitter.
 */
class OutboxQueueManager(
    private val bandejaSalidaMqttDao: BandejaSalidaMqttDao,
    private val mqttClientManager: MqttClientManager,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val maxQueueSize: Int = MAX_QUEUE_SIZE,
    private val maxRetries: Int = MAX_RETRY_ATTEMPTS,
) {
    private val tag = "OutboxQueueManager"
    private val drainMutex = Mutex()
    private var drainingJob: Job? = null

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    companion object {
        const val MAX_QUEUE_SIZE = 10_000
        const val MAX_RETRY_ATTEMPTS = 10
        const val INITIAL_BACKOFF_MS = 2_000L // 2 seconds
        const val MAX_BACKOFF_MS = 300_000L // 5 minutes
        const val JITTER_PERCENT = 0.20 // ±20%
        const val BATCH_SIZE = 100
    }

    init {
        mqttClientManager.setOnConnectedListener {
            startDraining()
        }
        scope.launch(ioDispatcher) {
            updatePendingCount()
        }
    }

    suspend fun getPendingCount(): Int = withContext(ioDispatcher) {
        bandejaSalidaMqttDao.getPendingCount()
    }

    private suspend fun updatePendingCount() {
        try {
            _pendingCount.value = bandejaSalidaMqttDao.getPendingCount()
        } catch (e: Exception) {
            Log.w(tag, "Failed to read pending count: ${e.message}")
        }
    }

    fun enqueue(topic: String, payload: String, qos: Int = 1, messageId: String = UUID.randomUUID().toString()) {
        scope.launch(ioDispatcher) {
            enqueueSuspend(topic, payload, qos, messageId)
        }
    }

    suspend fun enqueueSuspend(topic: String, payload: String, qos: Int = 1, messageId: String = UUID.randomUUID().toString()) = withContext(ioDispatcher) {
        try {
            if (bandejaSalidaMqttDao.messageExists(messageId)) {
                Log.d(tag, "Message already in outbox (idempotency check): $messageId")
                return@withContext
            }

            val message = BandejaSalidaMqttEntity(
                idMensaje = messageId,
                topico = topic,
                payload = payload,
                qos = qos,
                fechaHora = System.currentTimeMillis()
            )

            bandejaSalidaMqttDao.insert(message)
            Log.d(tag, "Message enqueued to outbox: $topic, messageId=$messageId")

            val count = bandejaSalidaMqttDao.getPendingCount()
            _pendingCount.value = count

            if (count > maxQueueSize) {
                val pruned = bandejaSalidaMqttDao.pruneToSize(maxQueueSize)
                updatePendingCount()
                Log.w(tag, "Queue exceeded max size ($maxQueueSize), pruned $pruned oldest messages")
            }

            if (mqttClientManager.isConnected.value) {
                startDraining()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to enqueue message to outbox: ${e.message}", e)
        }
    }

    fun startDraining() {
        if (drainingJob?.isActive == true) {
            return
        }
        drainingJob = scope.launch(ioDispatcher) {
            drainQueue()
        }
    }

    fun triggerDrain() = startDraining()

    fun stopDraining() {
        drainingJob?.cancel()
        drainingJob = null
        Log.i(tag, "Outbox queue draining stopped")
    }

    suspend fun drainQueue(): Int = withContext(ioDispatcher) {
        if (!mqttClientManager.isConnected.value) return@withContext 0
        if (!drainMutex.tryLock()) return@withContext 0

        var sentCount = 0
        try {
            Log.i(tag, "Starting outbox queue drain")

            while (isActive && mqttClientManager.isConnected.value) {
                val failedCount = bandejaSalidaMqttDao.markFailedAfterMaxRetries(maxRetries)
                if (failedCount > 0) {
                    Log.w(tag, "Marked $failedCount messages as FAILED after exceeding $maxRetries retries")
                }

                val pendingMessages = bandejaSalidaMqttDao.getPendingMessages(BATCH_SIZE)
                if (pendingMessages.isEmpty()) {
                    Log.d(tag, "Outbox queue is empty")
                    break
                }

                var processedInBatch = 0
                for (message in pendingMessages) {
                    if (!isActive || !mqttClientManager.isConnected.value) break

                    val backoffDelay = calculateBackoff(message.reintentos)
                    val now = System.currentTimeMillis()
                    if (message.ultimoReintento != null && (now - message.ultimoReintento) < backoffDelay) {
                        Log.d(tag, "Message ${message.idMensaje} waiting for backoff delay (${backoffDelay}ms)")
                        continue
                    }

                    val success = processMessage(message)
                    if (success) {
                        sentCount++
                    }
                    processedInBatch++
                }

                updatePendingCount()
                if (processedInBatch == 0 || !mqttClientManager.isConnected.value) {
                    break
                }
                delay(1000L)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error during outbox queue drain: ${e.message}", e)
        } finally {
            updatePendingCount()
            drainMutex.unlock()
            Log.i(tag, "Outbox queue drain finished, total sent: $sentCount")
        }
        sentCount
    }

    private suspend fun processMessage(message: BandejaSalidaMqttEntity): Boolean {
        val now = System.currentTimeMillis()
        return try {
            val result = mqttClientManager.publish(message.topico, message.payload, message.qos)
            if (result.isSuccess) {
                bandejaSalidaMqttDao.deleteById(message.id)
                Log.i(tag, "Message published and removed from outbox: ${message.topico}, messageId=${message.idMensaje}")
                true
            } else {
                val newRetryCount = message.reintentos + 1
                val errorMsg = result.exceptionOrNull()?.message ?: "Publish failed"
                bandejaSalidaMqttDao.updateRetryInfo(
                    id = message.id,
                    reintentos = newRetryCount,
                    timestamp = now,
                    error = errorMsg
                )
                Log.w(tag, "Publish failed for message ${message.idMensaje}, retryCount=$newRetryCount: $errorMsg")
                false
            }
        } catch (e: Exception) {
            val newRetryCount = message.reintentos + 1
            bandejaSalidaMqttDao.updateRetryInfo(
                id = message.id,
                reintentos = newRetryCount,
                timestamp = now,
                error = e.message
            )
            Log.e(tag, "Exception publishing outbox message ${message.idMensaje}: ${e.message}", e)
            false
        }
    }

    fun calculateBackoff(retryCount: Int): Long {
        val exponentialDelay = (INITIAL_BACKOFF_MS * (1 shl retryCount.coerceAtMost(10))).coerceAtMost(MAX_BACKOFF_MS)
        val jitterRange = (exponentialDelay * JITTER_PERCENT).toLong()
        val jitter = if (jitterRange > 0) ((Random.nextDouble() * 2 * jitterRange) - jitterRange).toLong() else 0L
        return (exponentialDelay + jitter).coerceIn(0L, MAX_BACKOFF_MS)
    }
}
