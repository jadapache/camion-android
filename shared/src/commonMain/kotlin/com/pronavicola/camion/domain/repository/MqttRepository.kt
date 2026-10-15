package com.pronavicola.camion.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface MqttRepository {
    val isConnected: StateFlow<Boolean>
    val outboxPendingCount: StateFlow<Int>
    fun start(brokerUrl: String, clientId: String, username: String = "", password: String = "")
    fun stop()
    suspend fun publish(topic: String, payload: String, qos: Int = 1): Boolean
    suspend fun enqueueOutbox(topic: String, payload: String, qos: Int = 1)
}
