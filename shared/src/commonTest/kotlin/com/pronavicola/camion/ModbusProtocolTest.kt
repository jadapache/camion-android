package com.pronavicola.camion

import com.pronavicola.camion.data.modbus.IEEE754Decoder
import com.pronavicola.camion.data.modbus.ModbusProtocol
import com.pronavicola.camion.domain.model.Micro820ModbusConstants
import com.pronavicola.camion.domain.model.ZoneStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModbusProtocolTest {

    @Test
    fun testBuildReadHoldingRegistersRequest() {
        // Read 12 holding registers starting at address 0 (40001), unitId 1, transactionId 0x1234
        val request = ModbusProtocol.buildReadHoldingRegistersRequest(
            transactionId = 0x1234,
            unitId = 1,
            startAddress = Micro820ModbusConstants.REG_ALARMAS,
            quantity = Micro820ModbusConstants.COUNT_HOLDING_REGS,
        )

        // MBAP: TxID (2), Proto (2), Length (2), UnitID (1) + PDU: FC (1), Addr (2), Qty (2) = 12 bytes
        assertEquals(12, request.size)
        assertEquals(0x12.toByte(), request[0])
        assertEquals(0x34.toByte(), request[1])
        assertEquals(0x00.toByte(), request[2]) // Protocol 0
        assertEquals(0x00.toByte(), request[3])
        assertEquals(0x00.toByte(), request[4]) // Length 6
        assertEquals(0x06.toByte(), request[5])
        assertEquals(0x01.toByte(), request[6]) // Unit ID 1
        assertEquals(0x03.toByte(), request[7]) // FC 03 (Holding Registers)
        assertEquals(0x00.toByte(), request[8]) // Start addr high
        assertEquals(0x00.toByte(), request[9]) // Start addr low
        assertEquals(0x00.toByte(), request[10]) // Qty high (0)
        assertEquals(0x0C.toByte(), request[11]) // Qty low (12)
    }

    @Test
    fun testBuildWriteSingleCoilRequest() {
        // Start pulse on Coil 1 (offset 0), value = true (0xFF00)
        val startPulseReq = ModbusProtocol.buildWriteSingleCoilRequest(
            transactionId = 0x1111,
            unitId = 1,
            coilAddress = Micro820ModbusConstants.COIL_INICIO,
            state = true,
        )
        assertEquals(12, startPulseReq.size)
        assertEquals(0x05.toByte(), startPulseReq[7]) // FC 05
        assertEquals(0x00.toByte(), startPulseReq[8]) // Coil addr high
        assertEquals(0x00.toByte(), startPulseReq[9]) // Coil addr low (0)
        assertEquals(0xFF.toByte(), startPulseReq[10]) // Value high 0xFF
        assertEquals(0x00.toByte(), startPulseReq[11]) // Value low 0x00

        // Stop pulse on Coil 3 (offset 2), value = true
        val stopPulseReq = ModbusProtocol.buildWriteSingleCoilRequest(
            transactionId = 0x2222,
            unitId = 1,
            coilAddress = Micro820ModbusConstants.COIL_DETENER,
            state = true,
        )
        assertEquals(0x05.toByte(), stopPulseReq[7]) // FC 05
        assertEquals(0x00.toByte(), stopPulseReq[8])
        assertEquals(0x02.toByte(), stopPulseReq[9]) // Coil addr low (2)
    }

    @Test
    fun testParseReadCoilsResponse() {
        // Fake response for 3 coils: Coil 2 (EN_PROCESO) ON -> 0b00000010 = 0x02
        val frame = byteArrayOf(
            0x00, 0x01, // TxID
            0x00, 0x00, // Proto
            0x00, 0x04, // Length
            0x01,       // Unit ID
            0x01,       // FC 01
            0x01,       // Byte Count = 1
            0x02.toByte(), // 0000 0010 -> coil 0: 0, coil 1: 1, coil 2: 0
        )

        val coils = ModbusProtocol.parseReadCoilsResponse(frame, 3)
        assertEquals(3, coils.size)
        assertFalse(coils[0]) // INICIO
        assertTrue(coils[1])  // EN_PROCESO
        assertFalse(coils[2]) // DETENER
    }

    @Test
    fun testParseReadHoldingRegistersResponse_TelemetryAndConfirmations() {
        // 12 registers (24 bytes):
        // Reg 0 (ALARMA): 0x0000
        // Reg 1 (CONFIRMAR): Bits 1, 2, 7, 8 ON -> (1<<1)|(1<<2)|(1<<7)|(1<<8) = 2 + 4 + 128 + 256 = 390 (0x0186)
        // Reg 2 (SALIDAS): 0x0000
        // Reg 3: 0x0000
        // Reg 4..5 (HUMEDAD): 65.0f -> 0x4282, 0x0000
        // Reg 6..7 (TEMP 1): 14.5f -> 0x4168, 0x0000
        // Reg 8..9 (TEMP 2): 15.0f -> 0x4170, 0x0000
        // Reg 10..11 (TENSION): 24.0f -> 0x41C0, 0x0000

        val (humHigh, humLow) = IEEE754Decoder.encodeIEEE754BigEndian(65.0f)
        val (t1High, t1Low) = IEEE754Decoder.encodeIEEE754BigEndian(14.5f)
        val (t2High, t2Low) = IEEE754Decoder.encodeIEEE754BigEndian(15.0f)
        val (vHigh, vLow) = IEEE754Decoder.encodeIEEE754BigEndian(24.0f)

        val registers = intArrayOf(
            0x0000,     // Reg 0: ALARMA
            0x0186,     // Reg 1: CONFIRMAR (Zona 1, Zona 2, Pulverizador, Damper ON)
            0x0000,     // Reg 2: SALIDAS
            0x0000,     // Reg 3
            humHigh, humLow,
            t1High, t1Low,
            t2High, t2Low,
            vHigh, vLow,
        )

        val pduBytes = mutableListOf<Byte>()
        for (r in registers) {
            pduBytes.add(((r ushr 8) and 0xFF).toByte())
            pduBytes.add((r and 0xFF).toByte())
        }

        val frame = ByteArray(7 + 2 + pduBytes.size).apply {
            this[0] = 0x00
            this[1] = 0x01
            this[2] = 0x00
            this[3] = 0x00
            this[4] = 0x00
            this[5] = (pduBytes.size + 3).toByte() // Length
            this[6] = 0x01 // Unit ID
            this[7] = 0x03 // FC 03
            this[8] = pduBytes.size.toByte() // Byte count
            for (i in pduBytes.indices) {
                this[9 + i] = pduBytes[i]
            }
        }

        val parsedRegs = ModbusProtocol.parseReadRegistersResponse(frame, 0x03)
        assertEquals(12, parsedRegs.size)

        // Verify decoded telemetry
        val decodedHum = IEEE754Decoder.decodeIEEE754BigEndian(parsedRegs[4], parsedRegs[5])
        val decodedT1 = IEEE754Decoder.decodeIEEE754BigEndian(parsedRegs[6], parsedRegs[7])
        val decodedT2 = IEEE754Decoder.decodeIEEE754BigEndian(parsedRegs[8], parsedRegs[9])
        val decodedV = IEEE754Decoder.decodeIEEE754BigEndian(parsedRegs[10], parsedRegs[11])

        assertEquals(65.0f, decodedHum)
        assertEquals(14.5f, decodedT1)
        assertEquals(15.0f, decodedT2)
        assertEquals(24.0f, decodedV)

        // Verify CONFIRMAR bit decoding
        val confirmar = parsedRegs[1]
        val zona1 = (confirmar and (1 shl Micro820ModbusConstants.BIT_CONFIRM_ZONA_1)) != 0
        val zona2 = (confirmar and (1 shl Micro820ModbusConstants.BIT_CONFIRM_ZONA_2)) != 0
        val zona3 = (confirmar and (1 shl Micro820ModbusConstants.BIT_CONFIRM_ZONA_3)) != 0
        val pulv = (confirmar and (1 shl Micro820ModbusConstants.BIT_CONFIRM_PULVERIZADOR)) != 0
        val damper = (confirmar and (1 shl Micro820ModbusConstants.BIT_CONFIRM_DAMPER)) != 0
        val calor = (confirmar and (1 shl Micro820ModbusConstants.BIT_CONFIRM_CALEFACCION)) != 0

        assertTrue(zona1)
        assertTrue(zona2)
        assertFalse(zona3)
        assertTrue(pulv)
        assertTrue(damper)
        assertFalse(calor)
    }
}
