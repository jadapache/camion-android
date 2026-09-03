package com.pronavicola.camion.data.mqtt

import android.util.Log
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
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Manages an in-memory outbox buffer for telemetry, alarm, and status messages,
 * ensuring QoS 1 delivery across network interruptions.
 */
class OutboxQueueManager(
    private val mqttManager: MqttClientManager,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val maxQueueSize: Int = 1000,
    private val maxRetries: Int = 5,
) {
    private val tag = "OutboxQueueManager"
    private val queue = ConcurrentLinkedQueue<OutboxMessage>()
    private val drainMutex = Mutex()
    private var drainJob: Job? = null

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    init {
        mqttManager.setOnConnectedListener {
            triggerDrain()
        }
        startPeriodicDrain()
    }

    fun enqueue(topic: String, payloadJson: String, qos: Int = 1) {
        if (queue.size >= maxQueueSize) {
            // Evict oldest item to prevent memory bloat
            queue.poll()
        }
        val message = OutboxMessage(
            topic = topic,
            payloadJson = payloadJson,
            qos = qos,
        )
        queue.offer(message)
        _pendingCount.value = queue.size

        if (mqttManager.isConnected.value) {
            triggerDrain()
        }
    }

    fun getPendingCount(): Int = queue.size

    fun triggerDrain() {
        scope.launch(ioDispatcher) {
            drainQueue()
        }
    }

    private fun startPeriodicDrain() {
        drainJob?.cancel()
        drainJob = scope.launch(ioDispatcher) {
            while (isActive) {
                if (mqttManager.isConnected.value && queue.isNotEmpty()) {
                    drainQueue()
                }
                delay(5000L)
            }
        }
    }

    suspend fun drainQueue(): Int = withContext(ioDispatcher) {
        if (!mqttManager.isConnected.value || queue.isEmpty()) return@withContext 0
        if (!drainMutex.tryLock()) return@withContext 0

        var sentCount = 0
        try {
            val batchLimit = 50
            var processed = 0

            while (queue.isNotEmpty() && processed < batchLimit && mqttManager.isConnected.value) {
                val message = queue.peek() ?: break
                val result = mqttManager.publish(message.topic, message.payloadJson, message.qos)

                if (result.isSuccess) {
                    queue.poll() // Successfully sent
                    _pendingCount.value = queue.size
                    sentCount++
                } else {
                    message.retryCount++
                    Log.w(tag, "Failed to send message to ${message.topic} (retry ${message.retryCount}): ${result.exceptionOrNull()?.message}")
                    if (message.retryCount >= maxRetries) {
                        Log.e(tag, "Discarding message to ${message.topic} after exceeding $maxRetries retries")
                        queue.poll()
                        _pendingCount.value = queue.size
                    }
                    break // Stop batch if network failed
                }
                processed++
            }
        } finally {
            _pendingCount.value = queue.size
            drainMutex.unlock()
        }
        sentCount
    }
}
