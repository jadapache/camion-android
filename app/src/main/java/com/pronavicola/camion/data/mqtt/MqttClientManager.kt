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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Enterprise-grade MQTT Client Manager using Eclipse Paho MQTT v3 with QoS 1,
 * automatic exponential backoff reconnection min(2^k * 1000ms, 60000ms),
 * and PUBACK delivery confirmation.
 */
class MqttClientManager(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultBrokerUrl: String = DEFAULT_BROKER_URL,
) {
    private val tag = "MqttClientManager"

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var mqttClient: MqttAsyncClient? = null
    private var connectionJob: Job? = null
    private val clientMutex = Mutex()

    private var currentBrokerUrl: String = defaultBrokerUrl
    private var currentClientId: String = "camion_${UUID.randomUUID().toString().take(8)}"
    private var onConnectedListener: (() -> Unit)? = null

    companion object {
        const val DEFAULT_BROKER_URL = "tcp://broker.emqx.io:1883"
        const val DEFAULT_QOS = 1
        const val MAX_BACKOFF_MS = 60000L

        /**
         * Calculates exponential backoff bounded at 60,000ms: min(2^k * 1000ms, 60000ms).
         */
        fun calculateBackoff(k: Int): Long {
            val exponent = k.coerceIn(0, 15)
            val delayMs = (1L shl exponent) * 1000L
            return minOf(delayMs, MAX_BACKOFF_MS)
        }
    }

    fun setOnConnectedListener(listener: () -> Unit) {
        onConnectedListener = listener
    }

    fun start(brokerUrl: String = currentBrokerUrl, clientId: String? = null) {
        currentBrokerUrl = brokerUrl
        if (clientId != null) {
            currentClientId = clientId
        }
        stop()

        connectionJob = scope.launch(ioDispatcher) {
            var attempt = 0
            while (isActive) {
                val success = tryConnect()
                if (success) {
                    attempt = 0
                    _isConnected.value = true
                    onConnectedListener?.invoke()
                    // Wait while connected
                    while (isActive && _isConnected.value) {
                        delay(1000L)
                    }
                } else {
                    _isConnected.value = false
                    val backoff = calculateBackoff(attempt)
                    Log.w(tag, "MQTT connection failed. Reconnecting in ${backoff}ms (attempt $attempt)...")
                    attempt++
                    delay(backoff)
                }
            }
        }
    }

    fun stop() {
        connectionJob?.cancel()
        connectionJob = null
        scope.launch(ioDispatcher) {
            disconnectInternal()
        }
    }

    private suspend fun tryConnect(): Boolean = clientMutex.withLock {
        try {
            disconnectInternal()
            val client = MqttAsyncClient(currentBrokerUrl, currentClientId, MemoryPersistence())
            mqttClient = client

            client.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.i(tag, "MQTT connected to $serverURI (reconnect=$reconnect)")
                    _isConnected.value = true
                    onConnectedListener?.invoke()
                }

                override fun connectionLost(cause: Throwable?) {
                    Log.w(tag, "MQTT connection lost: ${cause?.message}")
                    _isConnected.value = false
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                }
            })

            val options = MqttConnectOptions().apply {
                isCleanSession = false
                connectionTimeout = 15
                keepAliveInterval = 60
                isAutomaticReconnect = false // We control exponential backoff explicitly
            }

            suspendCancellableCoroutine<Boolean> { continuation ->
                try {
                    client.connect(options, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            continuation.resume(true)
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.w(tag, "MQTT connect onFailure: ${exception?.message}")
                            continuation.resume(false)
                        }
                    })
                } catch (e: Exception) {
                    continuation.resume(false)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "MQTT tryConnect exception: ${e.message}")
            false
        }
    }

    private fun disconnectInternal() {
        try {
            val client = mqttClient
            if (client != null && client.isConnected) {
                client.disconnect(1000)
            }
            client?.close()
        } catch (_: Exception) {
        } finally {
            mqttClient = null
            _isConnected.value = false
        }
    }

    /**
     * Publishes a message with QoS 1 and waits for PUBACK confirmation.
     */
    suspend fun publish(topic: String, payload: String, qos: Int = DEFAULT_QOS): Result<Unit> =
        withContext(ioDispatcher) {
            val client = mqttClient
            if (client == null || !client.isConnected) {
                return@withContext Result.failure(IllegalStateException("MQTT client is disconnected"))
            }

            try {
                val message = MqttMessage(payload.toByteArray(Charsets.UTF_8)).apply {
                    this.qos = qos
                    isRetained = false
                }

                suspendCancellableCoroutine { continuation ->
                    try {
                        client.publish(topic, message, null, object : IMqttActionListener {
                            override fun onSuccess(asyncActionToken: IMqttToken?) {
                                continuation.resume(Result.success(Unit))
                            }

                            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                                continuation.resume(
                                    Result.failure(exception ?: Exception("Publish failed without exception")),
                                )
                            }
                        })
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(e))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
