package com.pronavicola.camion.data.repository

import com.pronavicola.camion.data.mqtt.MqttClientManager
import com.pronavicola.camion.data.mqtt.OutboxQueueManager
import com.pronavicola.camion.domain.repository.MqttRepository
import kotlinx.coroutines.flow.StateFlow

class MqttRepositoryImpl(
    private val mqttClientManager: MqttClientManager,
    private val outboxQueueManager: OutboxQueueManager,
) : MqttRepository {

    override val isConnected: StateFlow<Boolean> = mqttClientManager.isConnected
    override val outboxPendingCount: StateFlow<Int> = outboxQueueManager.pendingCount

    override fun start(brokerUrl: String, clientId: String, username: String, password: String) {
        mqttClientManager.start(brokerUrl, clientId, username, password)
    }

    override fun stop() {
        mqttClientManager.stop()
    }

    override suspend fun publish(topic: String, payload: String, qos: Int): Boolean {
        return mqttClientManager.publish(topic, payload, qos).isSuccess
    }

    override suspend fun enqueueOutbox(topic: String, payload: String, qos: Int) {
        outboxQueueManager.enqueue(topic, payload, qos)
    }
}
