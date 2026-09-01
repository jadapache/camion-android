package com.pronavicola.camion.data.modbus

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Low-level Modbus TCP frame serializer and parser following the Modbus Application Protocol v1.1b.
 */
object ModbusProtocol {
    private val transactionIdCounter = AtomicInteger(1)

    fun nextTransactionId(): Int {
        val id = transactionIdCounter.incrementAndGet() and 0xFFFF
        if (id == 0) return transactionIdCounter.incrementAndGet() and 0xFFFF
        return id
    }

    /**
     * Builds MBAP Header + PDU for Read Coils (FC 01).
     * @param startAddress 0-indexed start address (e.g. 0 for address 1)
     * @param quantity number of coils to read (1..2000)
     */
    fun buildReadCoilsRequest(
        transactionId: Int = nextTransactionId(),
        unitId: Int = 1,
        startAddress: Int,
        quantity: Int,
    ): ByteArray {
        val pdu = ByteArrayOutputStream().apply {
            write(0x01) // FC 01
            write((startAddress ushr 8) and 0xFF)
            write(startAddress and 0xFF)
            write((quantity ushr 8) and 0xFF)
            write(quantity and 0xFF)
        }.toByteArray()

        return buildMbapFrame(transactionId, unitId, pdu)
    }

    /**
     * Builds MBAP Header + PDU for Read Holding Registers (FC 03).
     */
    fun buildReadHoldingRegistersRequest(
        transactionId: Int = nextTransactionId(),
        unitId: Int = 1,
        startAddress: Int,
        quantity: Int,
    ): ByteArray {
        val pdu = ByteArrayOutputStream().apply {
            write(0x03) // FC 03
            write((startAddress ushr 8) and 0xFF)
            write(startAddress and 0xFF)
            write((quantity ushr 8) and 0xFF)
            write(quantity and 0xFF)
        }.toByteArray()

        return buildMbapFrame(transactionId, unitId, pdu)
    }

    /**
     * Builds MBAP Header + PDU for Read Input Registers (FC 04).
     */
    fun buildReadInputRegistersRequest(
        transactionId: Int = nextTransactionId(),
        unitId: Int = 1,
        startAddress: Int,
        quantity: Int,
    ): ByteArray {
        val pdu = ByteArrayOutputStream().apply {
            write(0x04) // FC 04
            write((startAddress ushr 8) and 0xFF)
            write(startAddress and 0xFF)
            write((quantity ushr 8) and 0xFF)
            write(quantity and 0xFF)
        }.toByteArray()

        return buildMbapFrame(transactionId, unitId, pdu)
    }

    /**
     * Builds MBAP Header + PDU for Write Single Coil (FC 05).
     */
    fun buildWriteSingleCoilRequest(
        transactionId: Int = nextTransactionId(),
        unitId: Int = 1,
        coilAddress: Int,
        state: Boolean,
    ): ByteArray {
        val valueBytes = if (state) 0xFF00 else 0x0000
        val pdu = ByteArrayOutputStream().apply {
            write(0x05) // FC 05
            write((coilAddress ushr 8) and 0xFF)
            write(coilAddress and 0xFF)
            write((valueBytes ushr 8) and 0xFF)
            write(valueBytes and 0xFF)
        }.toByteArray()

        return buildMbapFrame(transactionId, unitId, pdu)
    }

    /**
     * Builds MBAP Header + PDU for Write Single Register (FC 06).
     */
    fun buildWriteSingleRegisterRequest(
        transactionId: Int = nextTransactionId(),
        unitId: Int = 1,
        registerAddress: Int,
        value: Int,
    ): ByteArray {
        val pdu = ByteArrayOutputStream().apply {
            write(0x06) // FC 06
            write((registerAddress ushr 8) and 0xFF)
            write(registerAddress and 0xFF)
            write((value ushr 8) and 0xFF)
            write(value and 0xFF)
        }.toByteArray()

        return buildMbapFrame(transactionId, unitId, pdu)
    }

    /**
     * Builds MBAP Header + PDU for Write Multiple Registers (FC 16 / 0x10).
     */
    fun buildWriteMultipleRegistersRequest(
        transactionId: Int = nextTransactionId(),
        unitId: Int = 1,
        startAddress: Int,
        values: List<Int>,
    ): ByteArray {
        val byteCount = values.size * 2
        val pdu = ByteArrayOutputStream().apply {
            write(0x10) // FC 16
            write((startAddress ushr 8) and 0xFF)
            write(startAddress and 0xFF)
            write((values.size ushr 8) and 0xFF)
            write(values.size and 0xFF)
            write(byteCount and 0xFF)
            for (v in values) {
                write((v ushr 8) and 0xFF)
                write(v and 0xFF)
            }
        }.toByteArray()

        return buildMbapFrame(transactionId, unitId, pdu)
    }

    private fun buildMbapFrame(transactionId: Int, unitId: Int, pdu: ByteArray): ByteArray {
        val length = pdu.size + 1 // +1 for Unit ID
        val frame = ByteArrayOutputStream()
        val out = DataOutputStream(frame)
        out.writeShort(transactionId)
        out.writeShort(0x0000) // Protocol ID
        out.writeShort(length)
        out.writeByte(unitId)
        out.write(pdu)
        return frame.toByteArray()
    }

    /**
     * Reads a full Modbus TCP response frame from the input stream.
     */
    fun readResponseFrame(inputStream: InputStream): ByteArray {
        val dataIn = DataInputStream(inputStream)
        val transactionId = dataIn.readUnsignedShort()
        val protocolId = dataIn.readUnsignedShort()
        val length = dataIn.readUnsignedShort()

        if (length < 2 || length > 260) {
            throw IllegalStateException("Invalid Modbus response length: $length")
        }

        val remaining = ByteArray(length)
        dataIn.readFully(remaining)

        val fullFrame = ByteArrayOutputStream().apply {
            val dOut = DataOutputStream(this)
            dOut.writeShort(transactionId)
            dOut.writeShort(protocolId)
            dOut.writeShort(length)
            write(remaining)
        }.toByteArray()

        return fullFrame
    }

    /**
     * Parses a response for Read Coils (FC 01).
     */
    fun parseReadCoilsResponse(frame: ByteArray, expectedCount: Int): List<Boolean> {
        checkErrorResponse(frame)
        val pduOffset = 7
        val functionCode = frame[pduOffset].toInt() and 0xFF
        if (functionCode != 0x01) {
            throw IllegalStateException("Expected FC 01 response, got: $functionCode")
        }
        val byteCount = frame[pduOffset + 1].toInt() and 0xFF
        val coils = mutableListOf<Boolean>()
        for (b in 0 until byteCount) {
            val byteVal = frame[pduOffset + 2 + b].toInt() and 0xFF
            for (bit in 0 until 8) {
                if (coils.size < expectedCount) {
                    coils.add(((byteVal ushr bit) and 0x01) == 1)
                }
            }
        }
        return coils
    }

    /**
     * Parses a response for Read Registers (FC 03 or FC 04).
     */
    fun parseReadRegistersResponse(frame: ByteArray, expectedFunctionCode: Int): List<Int> {
        checkErrorResponse(frame)
        val pduOffset = 7
        val functionCode = frame[pduOffset].toInt() and 0xFF
        if (functionCode != expectedFunctionCode) {
            throw IllegalStateException("Expected FC $expectedFunctionCode response, got: $functionCode")
        }
        val byteCount = frame[pduOffset + 1].toInt() and 0xFF
        val registerCount = byteCount / 2
        val registers = mutableListOf<Int>()
        for (i in 0 until registerCount) {
            val high = frame[pduOffset + 2 + (i * 2)].toInt() and 0xFF
            val low = frame[pduOffset + 2 + (i * 2) + 1].toInt() and 0xFF
            registers.add((high shl 8) or low)
        }
        return registers
    }

    /**
     * Checks if the response is a Modbus Exception (Function Code with MSB set).
     */
    fun checkErrorResponse(frame: ByteArray) {
        if (frame.size < 8) {
            throw IllegalStateException("Frame too short: ${frame.size} bytes")
        }
        val fc = frame[7].toInt() and 0xFF
        if ((fc and 0x80) != 0) {
            val exceptionCode = if (frame.size >= 9) frame[8].toInt() and 0xFF else 0
            val message = when (exceptionCode) {
                1 -> "Illegal Function"
                2 -> "Illegal Data Address"
                3 -> "Illegal Data Value"
                4 -> "Slave Device Failure"
                5 -> "Acknowledge"
                6 -> "Slave Device Busy"
                else -> "Modbus Exception Code $exceptionCode"
            }
            throw ModbusException(fc and 0x7F, exceptionCode, message)
        }
    }
}

class ModbusException(
    val functionCode: Int,
    val exceptionCode: Int,
    override val message: String,
) : Exception("Modbus error on FC $functionCode (0x${functionCode.toString(16)}): $message")
