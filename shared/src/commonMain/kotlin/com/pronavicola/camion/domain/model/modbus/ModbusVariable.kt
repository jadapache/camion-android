package com.pronavicola.camion.domain.model.modbus

/**
 * Supported Modbus data representations in the PLC Micro820.
 */
enum class ModbusDataType {
    /** 1-bit boolean coil or discrete input (FC 01, FC 05) */
    COIL,

    /** 16-bit word representing a bitfield (FC 03) where individual bits indicate states/alarms */
    BITMASK_16,

    /** 16-bit integer holding register (FC 03, FC 06, FC 16) */
    INT_16,

    /** 32-bit single precision IEEE-754 float spanning 2 consecutive holding registers in Big-Endian word order (FC 03, FC 16) */
    REAL_32,
}

/**
 * Definition of a Modbus PLC variable matching the legacy ModbusItem structure.
 *
 * @param id Unique identifier of the variable.
 * @param tag Canonical programmatic tag (e.g. "TempMax", "HumMin").
 * @param name User-friendly human readable label.
 * @param address 1-based PLC Modbus address (e.g. 1 for 40001 or Coil 1).
 * @param dataType Format and length of the register data.
 * @param unit Measurement unit string (e.g. "°C", "%", "V", "s").
 * @param bitIndex Zero-based bit position (0..15) if this variable represents a bit within a [ModbusDataType.BITMASK_16] word.
 * @param defaultValue Default starting value or setpoint.
 */
data class ModbusVariable(
    val id: Int,
    val tag: String,
    val name: String,
    val address: Int,
    val dataType: ModbusDataType,
    val unit: String = "",
    val bitIndex: Int? = null,
    val defaultValue: Double = 0.0,
) {
    /**
     * Converts 1-based Modbus PLC address into 0-based PDU offset for wire transmission.
     */
    val pduOffset: Int
        get() = (address - 1).coerceAtLeast(0)
}

