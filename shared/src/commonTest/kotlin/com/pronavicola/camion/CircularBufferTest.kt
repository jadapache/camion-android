package com.pronavicola.camion

import com.pronavicola.camion.util.CircularBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CircularBufferTest {

    @Test
    fun testBufferMaintainsCapacityAndEvictsOldest() {
        val buffer = CircularBuffer<String>(3)
        assertEquals(0, buffer.size)

        buffer.add("A")
        buffer.add("B")
        buffer.add("C")

        assertEquals(3, buffer.size)
        assertTrue(buffer.contains("A"))
        assertTrue(buffer.contains("B"))
        assertTrue(buffer.contains("C"))
        assertEquals(listOf("A", "B", "C"), buffer.toList())

        // Adding 4th element should evict "A"
        buffer.add("D")
        assertEquals(3, buffer.size)
        assertFalse(buffer.contains("A"))
        assertTrue(buffer.contains("B"))
        assertTrue(buffer.contains("C"))
        assertTrue(buffer.contains("D"))
        assertEquals(setOf("B", "C", "D"), buffer.toList().toSet())
    }

    @Test
    fun testBufferClear() {
        val buffer = CircularBuffer<Int>(5)
        buffer.add(1)
        buffer.add(2)
        buffer.add(3)

        assertEquals(3, buffer.size)
        buffer.clear()
        assertEquals(0, buffer.size)
        assertFalse(buffer.contains(1))
        assertTrue(buffer.toList().isEmpty())
    }

    @Test
    fun testLargeCapacityIdempotencyGuard() {
        val buffer = CircularBuffer<String>(1000)
        for (i in 0 until 1500) {
            buffer.add("item_$i")
        }

        assertEquals(1000, buffer.size)
        assertFalse(buffer.contains("item_0"))
        assertFalse(buffer.contains("item_499"))
        assertTrue(buffer.contains("item_500"))
        assertTrue(buffer.contains("item_1499"))
    }
}
