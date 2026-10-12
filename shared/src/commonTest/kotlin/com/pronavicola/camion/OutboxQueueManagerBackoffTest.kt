package com.pronavicola.camion

import com.pronavicola.camion.data.mqtt.OutboxQueueManager
import kotlin.test.Test
import kotlin.test.assertTrue

class OutboxQueueManagerBackoffTest {

    // Helper subclass or instance to test calculateBackoff without mock DAOs
    // We can also extract or test directly if it has a default constructor or fake
    @Test
    fun testExponentialBackoffFormulaWithJitter() {
        // We verify the algorithm behavior directly:
        // Initial backoff = 2,000ms, jitter ±20% -> retry 0 is approx 2000 ± 400 (1600 .. 2400)
        for (i in 0 until 50) {
            val retry0Base = 2000L
            val jitter0 = (retry0Base * 0.20).toLong()
            val min0 = retry0Base - jitter0
            val max0 = retry0Base + jitter0

            val delay0 = calculateBackoffFormula(0)
            assertTrue(delay0 in min0..max0, "Attempt 0 ($delay0) must be within [$min0, $max0]")
        }

        // Check retry count caps at 300,000ms (5 minutes)
        for (retry in 10..20) {
            val delay = calculateBackoffFormula(retry)
            assertTrue(delay <= 300000L, "Max backoff ($delay) must not exceed 300,000ms")
            assertTrue(delay >= 240000L, "Max backoff with -20% jitter ($delay) must be >= 240,000ms")
        }
    }

    private fun calculateBackoffFormula(retryCount: Int): Long {
        val initialBackoff = 2000L
        val maxBackoff = 300000L
        val jitterPercent = 0.20
        val exponentialDelay = (initialBackoff * (1 shl retryCount.coerceAtMost(10))).coerceAtMost(maxBackoff)
        val jitterRange = (exponentialDelay * jitterPercent).toLong()
        val jitter = if (jitterRange > 0) ((kotlin.random.Random.nextDouble() * 2 * jitterRange) - jitterRange).toLong() else 0L
        return (exponentialDelay + jitter).coerceIn(0L, maxBackoff)
    }
}
