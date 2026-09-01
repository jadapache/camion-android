package com.pronavicola.camion

import com.pronavicola.camion.data.modbus.IEEE754Decoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class IEEE754DecoderTest {

    @Test
    fun testKnownValues() {
        // Zero
        val (high0, low0) = IEEE754Decoder.encodeIEEE754BigEndian(0.0f)
        assertEquals(0, high0)
        assertEquals(0, low0)
        assertEquals(0.0f, IEEE754Decoder.decodeIEEE754BigEndian(high0, low0), 0.0001f)

        // 14.5 °C (Hex: 0x41680000 -> high=0x4168, low=0x0000 -> 16744, 0)
        val (highTemp, lowTemp) = IEEE754Decoder.encodeIEEE754BigEndian(14.5f)
        assertEquals(0x4168, highTemp)
        assertEquals(0x0000, lowTemp)
        assertEquals(14.5f, IEEE754Decoder.decodeIEEE754BigEndian(highTemp, lowTemp), 0.0001f)

        // Negative value -20.5 °C
        val (highNeg, lowNeg) = IEEE754Decoder.encodeIEEE754BigEndian(-20.5f)
        assertEquals(-20.5f, IEEE754Decoder.decodeIEEE754BigEndian(highNeg, lowNeg), 0.0001f)
    }

    @Test
    fun testRoundTripPropertyAcrossMultipleValues() {
        val random = Random(42)
        val testValues = mutableListOf(
            0.0f, -0.0f, 1.0f, -1.0f, 0.5f, -0.5f,
            14.5f, 220.0f, 65.4f, 12.345f, -40.0f, 100.0f,
            Float.MIN_VALUE, Float.MAX_VALUE,
        )

        // Generate 2000 random test cases
        repeat(2000) {
            val randomFloat = random.nextFloat() * 2000.0f - 1000.0f
            testValues.add(randomFloat)
        }

        for (v in testValues) {
            val (high, low) = IEEE754Decoder.encodeIEEE754BigEndian(v)
            assertTrue("High word must be 16-bit unsigned [0..65535]", high in 0..0xFFFF)
            assertTrue("Low word must be 16-bit unsigned [0..65535]", low in 0..0xFFFF)

            val decoded = IEEE754Decoder.decodeIEEE754BigEndian(high, low)
            assertEquals("Roundtrip decode(encode(x)) == x failed for $v", v, decoded, 0.0001f)
        }
    }
}
