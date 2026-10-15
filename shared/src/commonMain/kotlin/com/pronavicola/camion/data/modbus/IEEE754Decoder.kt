package com.pronavicola.camion.data.modbus

/**
 * Encodes and decodes 32-bit single-precision floating point numbers (IEEE 754)
 * to and from pairs of 16-bit Modbus registers in Big-Endian word order.
 */
object IEEE754Decoder {

    /**
     * Decodes two 16-bit registers into a 32-bit Float using Big-Endian word order
     * (high word first, low word second).
     */
    fun decodeIEEE754BigEndian(highWord: Int, lowWord: Int): Float {
        val combined = ((highWord and 0xFFFF) shl 16) or (lowWord and 0xFFFF)
        return java.lang.Float.intBitsToFloat(combined)
    }

    /**
     * Encodes a 32-bit Float into two 16-bit registers using Big-Endian word order.
     * Returns a Pair of (highWord, lowWord).
     */
    fun encodeIEEE754BigEndian(value: Float): Pair<Int, Int> {
        val bits = java.lang.Float.floatToIntBits(value)
        val highWord = (bits ushr 16) and 0xFFFF
        val lowWord = bits and 0xFFFF
        return Pair(highWord, lowWord)
    }

    /**
     * Decodes a list of 16-bit register words into a list of Float values (each 2 words = 1 Float).
     */
    fun decodeFloatArray(words: List<Int>): List<Float> {
        val result = mutableListOf<Float>()
        var i = 0
        while (i + 1 < words.size) {
            result.add(decodeIEEE754BigEndian(words[i], words[i + 1]))
            i += 2
        }
        return result
    }
}
