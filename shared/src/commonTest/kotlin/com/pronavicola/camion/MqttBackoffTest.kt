package com.pronavicola.camion

import com.pronavicola.camion.data.mqtt.MqttClientManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MqttBackoffTest {

    @Test
    fun testExponentialBackoffValues() {
        // Attempt 0: 1000ms
        assertEquals(1000L, MqttClientManager.calculateBackoff(0))
        // Attempt 1: 2000ms
        assertEquals(2000L, MqttClientManager.calculateBackoff(1))
        // Attempt 2: 4000ms
        assertEquals(4000L, MqttClientManager.calculateBackoff(2))
        // Attempt 3: 8000ms
        assertEquals(8000L, MqttClientManager.calculateBackoff(3))
        // Attempt 4: 16000ms
        assertEquals(16000L, MqttClientManager.calculateBackoff(4))
        // Attempt 5: 32000ms
        assertEquals(32000L, MqttClientManager.calculateBackoff(5))
        // Attempt 6+: capped at 60000ms
        assertEquals(60000L, MqttClientManager.calculateBackoff(6))
        assertEquals(60000L, MqttClientManager.calculateBackoff(10))
        assertEquals(60000L, MqttClientManager.calculateBackoff(50))
    }

    @Test
    fun testBackoffBoundedProperty() {
        for (k in 0..100) {
            val backoff = MqttClientManager.calculateBackoff(k)
            assertTrue(backoff > 0L, "Backoff must be positive")
            assertTrue(backoff <= 60000L, "Backoff must never exceed 60,000ms")
        }
    }
}
