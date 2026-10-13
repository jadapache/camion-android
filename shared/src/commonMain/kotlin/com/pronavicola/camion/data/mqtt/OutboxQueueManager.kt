package com.pronavicola.camion.data.mqtt

import com.pronavicola.camion.data.local.dao.BuzonMqttDao
import com.pronavicola.camion.data.local.dao.VariableRegistroDao
import com.pronavicola.camion.data.local.entity.BuzonMqttEntity
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
import java.util.UUID

/**
 * Manages an offline-first persistent Room outbox buffer for telemetry, GPS, and alarm messages,
 * ensuring QoS 1 delivery across network interruptions and app restarts.
 */
class OutboxQueueManager(
    private val mqttManager: MqttClientManager,
    private val buzonMqttDao: BuzonMqttDao,
    private val variableRegistroDao: VariableRegistroDao? = null,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val maxQueueSize: Int = 10000,
    private val maxRetries: Int = 10,
) {
    private val tag = "OutboxQueueManager"
    private val drainMutex = Mutex()
    private var drainJob: Job? = null

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    init {
        mqttManager.setOnConnectedListener {
            triggerDrain()
        }
        scope.launch(ioDispatcher) {
            refreshPendingCount()
        }
        startPeriodicDrain()
    }

    suspend fun enqueue(topic: String, payloadJson: String, qos: Int = 1) {
        val currentCount = buzonMqttDao.getPendingCount()
        if (currentCount >= maxQueueSize) {
            // Prune oldest records to keep buffer bounded
            buzonMqttDao.pruneOldest(100)
        }

        val entity = BuzonMqttEntity(
            idMensaje = UUID.randomUUID().toString(),
            topico = topic,
            cargaUtil = payloadJson,
            qos = qos,
            marcaTiempo = System.currentTimeMillis(),
            conteoReintentos = 0,
            estado = "PENDING"
        )

        buzonMqttDao.insert(entity)
        refreshPendingCount()

        if (mqttManager.isConnected.value) {
            triggerDrain()
        }
    }

    fun getPendingCount(): Int = _pendingCount.value

    fun triggerDrain() {
        scope.launch(ioDispatcher) {
            drainQueue()
        }
    }

    private suspend fun refreshPendingCount() {
        try {
            _pendingCount.value = buzonMqttDao.getPendingCount()
        } catch (e: Exception) {
            Log.e(tag, "Failed to count pending outbox messages: ${e.message}")
        }
    }

    private fun startPeriodicDrain() {
        drainJob?.cancel()
        drainJob = scope.launch(ioDispatcher) {
            while (isActive) {
                if (mqttManager.isConnected.value) {
                    drainQueue()
                }
                delay(5000L)
            }
        }
    }

    suspend fun drainQueue(): Int {
        if (!mqttManager.isConnected.value) return 0
        if (!drainMutex.tryLock()) return 0

        var sentCount = 0
        try {
            val pendingMessages = buzonMqttDao.getPendingMessages(limit = 50)
            if (pendingMessages.isEmpty()) return 0

            for (msg in pendingMessages) {
                if (!mqttManager.isConnected.value) break

                val result = mqttManager.publish(msg.topico, msg.cargaUtil, msg.qos)
                if (result.isSuccess) {
                    buzonMqttDao.deleteById(msg.id)
                    sentCount++
                } else {
                    val retries = msg.conteoReintentos + 1
                    val errorMsg = result.exceptionOrNull()?.message ?: "Publish failed"
                    buzonMqttDao.updateRetryInfo(msg.id, System.currentTimeMillis(), errorMsg)

                    if (retries >= maxRetries) {
                        Log.e(tag, "Message ${msg.id} marked FAILED after $retries retries")
                        buzonMqttDao.updateStatus(msg.id, "FAILED")
                    }
                    break // Stop batch on network error
                }
            }

            // Once outbox messages are delivered to the broker, mark any pending SQLite records as Enviado
            if (sentCount > 0) {
                variableRegistroDao?.markAllPendingAsEnviado()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error during outbox drain: ${e.message}")
        } finally {
            refreshPendingCount()
            drainMutex.unlock()
        }
        return sentCount
    }
}

