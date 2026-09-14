package com.pronavicola.camion

import com.pronavicola.camion.data.modbus.ModbusProtocol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.io.ByteArrayInputStream

class ModbusProtocolTest {

    @Test
    fun testBuildReadInputRegistersRequest() {
        // Read 8 input registers starting at address 0, unitId 1, transactionId 0x1234
        val request = ModbusProtocol.buildReadInputRegistersRequest(
            transactionId = 0x1234,
            unitId = 1,
            startAddress = 0,
            quantity = 8,
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
        assertEquals(0x04.toByte(), request[7]) // FC 04
        assertEquals(0x00.toByte(), request[8]) // Start addr high
        assertEquals(0x00.toByte(), request[9]) // Start addr low
        assertEquals(0x00.toByte(), request[10]) // Qty high
        assertEquals(0x08.toByte(), request[11]) // Qty low
    }

    @Test
    fun testParseReadCoilsResponse() {
        // Fake response for 8 coils: Coils 0, 1, 3, 5 ON -> 0b00101011 = 0x2B (43)
        // Frame: MBAP (7 bytes) + FC 01 + ByteCount (1) + CoilData (0x2B)
        val frame = byteArrayOf(
            0x00, 0x01, // TxID
            0x00, 0x00, // Proto
            0x00, 0x04, // Length (UnitID + FC + ByteCount + Data = 4)
            0x01,       // Unit ID
            0x01,       // FC 01
            0x01,       // Byte Count = 1
            0x2B.toByte(), // 0010 1011
        )

        val coils = ModbusProtocol.parseReadCoilsResponse(frame, 8)
        assertEquals(8, coils.size)
        assertTrue(coils[0])  // bit 0 (1)
        assertTrue(coils[1])  // bit 1 (1)
        assertFalse(coils[2]) // bit 2 (0)
        assertTrue(coils[3])  // bit 3 (1)
        assertFalse(coils[4]) // bit 4 (0)
        assertTrue(coils[5])  // bit 5 (1)
        assertFalse(coils[6]) // bit 6 (0)
        assertFalse(coils[7]) // bit 7 (0)
    }

    @Test
    fun testParseReadRegistersResponse() {
        // Frame: MBAP (7 bytes) + FC 04 + ByteCount (4) + Reg0 (0x41, 0x68) + Reg1 (0x00, 0x00) -> 14.5f
        val frame = byteArrayOf(
            0x00, 0x01,
            0x00, 0x00,
            0x00, 0x07, // Length (UnitID + FC + ByteCount + 4 bytes = 7)
            0x01,       // Unit ID
            0x04,       // FC 04
            0x04,       // Byte Count = 4
            0x41, 0x68, // Reg 0: 0x4168
            0x00, 0x00, // Reg 1: 0x0000
        )

        val regs = ModbusProtocol.parseReadRegistersResponse(frame, 0x04)
        assertEquals(2, regs.size)
        assertEquals(0x4168, regs[0])
        assertEquals(0x0000, regs[1])
    }
}
