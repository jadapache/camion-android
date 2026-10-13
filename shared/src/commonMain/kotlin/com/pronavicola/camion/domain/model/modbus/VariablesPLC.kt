package com.pronavicola.camion.domain.model.modbus

/**
 * Central registry of PLC Modbus variables for the Truck Monitoring & Control System.
 *
 * Faithfully mirrors the legacy `VariablesPLC.vb` and `PLCAddress.xaml` configurations,
 * offering strongly-typed metadata for commands, telemetry, parameters, and alarm bitmasks.
 */
object VariablesPLC {

    // =========================================================================
    // 1. COMMANDS & DISCRETE STATES (Coils: FC 01 / FC 05)
    // =========================================================================
    object Commands {
        val Iniciar = ModbusVariable(
            id = 1,
            tag = "Iniciar",
            name = "Iniciar Proceso",
            address = 1,
            dataType = ModbusDataType.COIL,
            defaultValue = 1.0,
        )

        val EstadoEnProceso = ModbusVariable(
            id = 2,
            tag = "Estado",
            name = "Proceso en Ejecución",
            address = 2,
            dataType = ModbusDataType.COIL,
        )

        val Detener = ModbusVariable(
            id = 3,
            tag = "Detener",
            name = "Detener Proceso",
            address = 3,
            dataType = ModbusDataType.COIL,
            defaultValue = 1.0,
        )

        val all = listOf(Iniciar, EstadoEnProceso, Detener)
    }

    // =========================================================================
    // 2. REAL-TIME TELEMETRY & BITMASKS (Holding Registers: 40001..40012)
    // =========================================================================
    object Telemetry {
        val RegistroAlarmas = ModbusVariable(
            id = 100,
            tag = "REG_ALARMAS",
            name = "Palabra de Alarmas",
            address = 1, // 40001 (PDU offset 0)
            dataType = ModbusDataType.BITMASK_16,
        )

        val RegistroConfirmar = ModbusVariable(
            id = 101,
            tag = "REG_CONFIRMAR",
            name = "Palabra de Confirmaciones",
            address = 2, // 40002 (PDU offset 1)
            dataType = ModbusDataType.BITMASK_16,
        )

        val RegistroSalidas = ModbusVariable(
            id = 102,
            tag = "REG_SALIDAS",
            name = "Palabra de Salidas Físicas",
            address = 3, // 40003 (PDU offset 2)
            dataType = ModbusDataType.BITMASK_16,
        )

        val Humedad = ModbusVariable(
            id = 103,
            tag = "Humedad",
            name = "Humedad Relativa",
            address = 5, // 40005..40006 (PDU offset 4)
            dataType = ModbusDataType.REAL_32,
            unit = "%",
        )

        val Temperatura1 = ModbusVariable(
            id = 104,
            tag = "Temperatura1",
            name = "Temperatura Sensor 1",
            address = 7, // 40007..40008 (PDU offset 6)
            dataType = ModbusDataType.REAL_32,
            unit = "°C",
        )

        val Temperatura2 = ModbusVariable(
            id = 105,
            tag = "Temperatura2",
            name = "Temperatura Sensor 2",
            address = 9, // 40009..40010 (PDU offset 8)
            dataType = ModbusDataType.REAL_32,
            unit = "°C",
        )

        val Tension = ModbusVariable(
            id = 106,
            tag = "Tension",
            name = "Tensión de Batería",
            address = 11, // 40011..40012 (PDU offset 10)
            dataType = ModbusDataType.REAL_32,
            unit = "VDC",
        )

        val all = listOf(RegistroAlarmas, RegistroConfirmar, RegistroSalidas, Humedad, Temperatura1, Temperatura2, Tension)
    }

    // =========================================================================
    // 3. PROCESS PARAMETERS (Holding Registers: 40029..40077)
    // Matches `VariablesPLC.Parameters` in legacy system
    // =========================================================================
    object Parameters {
        val TempMin = ModbusVariable(
            id = 4,
            tag = "TempMin",
            name = "Temperatura Mínima",
            address = 29, // 40029 (PDU 28)
            dataType = ModbusDataType.REAL_32,
            unit = "°C",
            defaultValue = 12.0,
        )

        val TempMax = ModbusVariable(
            id = 3,
            tag = "TempMax",
            name = "Temperatura Máxima",
            address = 31, // 40031 (PDU 30)
            dataType = ModbusDataType.REAL_32,
            unit = "°C",
            defaultValue = 18.0,
        )

        val HumMin = ModbusVariable(
            id = 2,
            tag = "HumMin",
            name = "Humedad Mínima",
            address = 33, // 40033 (PDU 32)
            dataType = ModbusDataType.REAL_32,
            unit = "%",
            defaultValue = 55.0,
        )

        val HumMax = ModbusVariable(
            id = 1,
            tag = "HumMax",
            name = "Humedad Máxima",
            address = 35, // 40035 (PDU 34)
            dataType = ModbusDataType.REAL_32,
            unit = "%",
            defaultValue = 75.0,
        )

        val DamperTiempoOff = ModbusVariable(
            id = 12,
            tag = "DamperTiempoOff",
            name = "Tiempo OFF Damper",
            address = 37, // 40037 (PDU 36)
            dataType = ModbusDataType.REAL_32,
            unit = "s",
            defaultValue = 30.0,
        )

        val DamperTiempoOn = ModbusVariable(
            id = 11,
            tag = "DamperTiempoOn",
            name = "Tiempo ON Damper",
            address = 39, // 40039 (PDU 38)
            dataType = ModbusDataType.REAL_32,
            unit = "s",
            defaultValue = 15.0,
        )

        val PulverizadorTiempoOff = ModbusVariable(
            id = 6,
            tag = "PulverizadorTiempoOff",
            name = "Tiempo OFF Pulverizador",
            address = 41, // 40041 (PDU 40)
            dataType = ModbusDataType.REAL_32,
            unit = "s",
            defaultValue = 20.0,
        )

        val PulverizadorTiempoOn = ModbusVariable(
            id = 5,
            tag = "PulverizadorTiempoOn",
            name = "Tiempo ON Pulverizador",
            address = 43, // 40043 (PDU 42)
            dataType = ModbusDataType.REAL_32,
            unit = "s",
            defaultValue = 10.0,
        )

        // Calibration / Scaler parameters
        val Scaler1FactorMinH = ModbusVariable(
            id = 13,
            tag = "SCALER_1_FactorMinH",
            name = "Factor Min Humedad",
            address = 45,
            dataType = ModbusDataType.REAL_32,
        )

        val Scaler1FactorMaxH = ModbusVariable(
            id = 14,
            tag = "SCALER_1_FactorMaxH",
            name = "Factor Max Humedad",
            address = 47,
            dataType = ModbusDataType.REAL_32,
        )

        val Scaler1RangoMinH = ModbusVariable(
            id = 15,
            tag = "SCALER_1_RangoMinH",
            name = "Rango Min Humedad",
            address = 49,
            dataType = ModbusDataType.REAL_32,
        )

        val Scaler1RangoMaxH = ModbusVariable(
            id = 16,
            tag = "SCALER_1_RangoMaxH",
            name = "Rango Max Humedad",
            address = 51,
            dataType = ModbusDataType.REAL_32,
        )

        val Scaler2FactorMinT1 = ModbusVariable(
            id = 17,
            tag = "SCALER_2_FactorMinT1",
            name = "Factor Min Temp 1",
            address = 53,
            dataType = ModbusDataType.REAL_32,
        )

        val Scaler2FactorMaxT1 = ModbusVariable(
            id = 18,
            tag = "SCALER_2_FactorMaxT1",
            name = "Factor Max Temp 1",
            address = 55,
            dataType = ModbusDataType.REAL_32,
        )

        val Scaler2RangoMinT1 = ModbusVariable(
            id = 19,
            tag = "SCALER_2_RangoMinT1",
            name = "Rango Min Temp 1",
            address = 57,
            dataType = ModbusDataType.REAL_32,
        )

        val Scaler2RangoMaxT1 = ModbusVariable(
            id = 20,
            tag = "SCALER_2_RangoMaxT1",
            name = "Rango Max Temp 1",
            address = 59,
            dataType = ModbusDataType.REAL_32,
        )

        val Scaler3FactorMinT2 = ModbusVariable(
            id = 21,
            tag = "SCALER_3_FactorMinT2",
            name = "Factor Min Temp 2",
            address = 61,
            dataType = ModbusDataType.REAL_32,
        )

        val Scaler3FactorMaxT2 = ModbusVariable(
            id = 22,
            tag = "SCALER_3_FactorMaxT2",
            name = "Factor Max Temp 2",
            address = 63,
            dataType = ModbusDataType.REAL_32,
        )

        val Scaler3RangoMinT2 = ModbusVariable(
            id = 23,
            tag = "SCALER_3_RangoMinT2",
            name = "Rango Min Temp 2",
            address = 65,
            dataType = ModbusDataType.REAL_32,
        )

        val Scaler3RangoMaxT2 = ModbusVariable(
            id = 24,
            tag = "SCALER_3_RangoMaxT2",
            name = "Rango Max Temp 2",
            address = 67,
            dataType = ModbusDataType.REAL_32,
        )

        val Scaler4FactorMinV = ModbusVariable(
            id = 25,
            tag = "SCALER_4_FactorMinV",
            name = "Factor Min Tensión",
            address = 69,
            dataType = ModbusDataType.REAL_32,
        )

        val Scaler4FactorMaxV = ModbusVariable(
            id = 26,
            tag = "SCALER_4_FactorMaxV",
            name = "Factor Max Tensión",
            address = 71,
            dataType = ModbusDataType.REAL_32,
        )

        val Scaler4RangoMinV = ModbusVariable(
            id = 27,
            tag = "SCALER_4_RangoMinV",
            name = "Rango Min Tensión",
            address = 73,
            dataType = ModbusDataType.REAL_32,
        )

        val Scaler4RangoMaxV = ModbusVariable(
            id = 28,
            tag = "SCALER_4_RangoMaxV",
            name = "Rango Max Tensión",
            address = 75,
            dataType = ModbusDataType.REAL_32,
        )

        val TemSelect = ModbusVariable(
            id = 29,
            tag = "Tem_Select",
            name = "Selector de Sensor de Temperatura",
            address = 77,
            dataType = ModbusDataType.INT_16,
        )

        val all = listOf(
            HumMax, HumMin, TempMax, TempMin,
            PulverizadorTiempoOn, PulverizadorTiempoOff,
            DamperTiempoOn, DamperTiempoOff,
            Scaler1FactorMinH, Scaler1FactorMaxH, Scaler1RangoMinH, Scaler1RangoMaxH,
            Scaler2FactorMinT1, Scaler2FactorMaxT1, Scaler2RangoMinT1, Scaler2RangoMaxT1,
            Scaler3FactorMinT2, Scaler3FactorMaxT2, Scaler3RangoMinT2, Scaler3RangoMaxT2,
            Scaler4FactorMinV, Scaler4FactorMaxV, Scaler4RangoMinV, Scaler4RangoMaxV,
            TemSelect
        )
    }

    // =========================================================================
    // 4. ALARMS BITMAP (Address 40001 / REG_ALARMAS)
    // Matches `VariablesPLC.Alarms` in legacy system
    // =========================================================================
    object Alarms {
        val FallaEnergia = ModbusVariable(id = 1, tag = "Alarma_FallaEnergia", name = "falla de energia", address = 1, dataType = ModbusDataType.BITMASK_16, bitIndex = 0)
        val Zona1 = ModbusVariable(id = 2, tag = "Alarma_Zona1", name = "zona 1 no enciende", address = 1, dataType = ModbusDataType.BITMASK_16, bitIndex = 1)
        val Zona2 = ModbusVariable(id = 3, tag = "Alarma_Zona2", name = "zona 2 no enciende", address = 1, dataType = ModbusDataType.BITMASK_16, bitIndex = 2)
        val Zona3 = ModbusVariable(id = 4, tag = "Alarma_Zona3", name = "zona 3 no enciende", address = 1, dataType = ModbusDataType.BITMASK_16, bitIndex = 3)
        val Zona4 = ModbusVariable(id = 5, tag = "Alarma_Zona4", name = "zona 4 no enciende", address = 1, dataType = ModbusDataType.BITMASK_16, bitIndex = 4)
        val Zona5 = ModbusVariable(id = 6, tag = "Alarma_Zona5", name = "zona 5 no enciende", address = 1, dataType = ModbusDataType.BITMASK_16, bitIndex = 5)
        val Zona6 = ModbusVariable(id = 7, tag = "Alarma_Zona6", name = "zona 6 no enciende", address = 1, dataType = ModbusDataType.BITMASK_16, bitIndex = 6)
        val Pulverizador = ModbusVariable(id = 8, tag = "Alarma_Pulverizador", name = "no humifica", address = 1, dataType = ModbusDataType.BITMASK_16, bitIndex = 7)
        val Damper = ModbusVariable(id = 9, tag = "Alarma_Damper", name = "damper no abre", address = 1, dataType = ModbusDataType.BITMASK_16, bitIndex = 8)
        val Calefaccion = ModbusVariable(id = 10, tag = "Alarma_Calefaccion", name = "calefaccion no enciende", address = 1, dataType = ModbusDataType.BITMASK_16, bitIndex = 9)
        val NivelAguaBajo = ModbusVariable(id = 11, tag = "Alarma_NivelAguaBajo", name = "nivel de agua bajo", address = 1, dataType = ModbusDataType.BITMASK_16, bitIndex = 10)

        val all = listOf(FallaEnergia, Zona1, Zona2, Zona3, Zona4, Zona5, Zona6, Pulverizador, Damper, Calefaccion, NivelAguaBajo)
    }

    // =========================================================================
    // 5. CONFIRMATION BITMAP (Address 40002 / REG_CONFIRMAR)
    // Actuator feedback verification
    // =========================================================================
    object Confirmations {
        val Zona1 = ModbusVariable(id = 201, tag = "Confirm_Zona1", name = "Confirmación Zona 1", address = 2, dataType = ModbusDataType.BITMASK_16, bitIndex = 1)
        val Zona2 = ModbusVariable(id = 202, tag = "Confirm_Zona2", name = "Confirmación Zona 2", address = 2, dataType = ModbusDataType.BITMASK_16, bitIndex = 2)
        val Zona3 = ModbusVariable(id = 203, tag = "Confirm_Zona3", name = "Confirmación Zona 3", address = 2, dataType = ModbusDataType.BITMASK_16, bitIndex = 3)
        val Zona4 = ModbusVariable(id = 204, tag = "Confirm_Zona4", name = "Confirmación Zona 4", address = 2, dataType = ModbusDataType.BITMASK_16, bitIndex = 4)
        val Zona5 = ModbusVariable(id = 205, tag = "Confirm_Zona5", name = "Confirmación Zona 5", address = 2, dataType = ModbusDataType.BITMASK_16, bitIndex = 5)
        val Zona6 = ModbusVariable(id = 206, tag = "Confirm_Zona6", name = "Confirmación Zona 6", address = 2, dataType = ModbusDataType.BITMASK_16, bitIndex = 6)
        val Pulverizador = ModbusVariable(id = 207, tag = "Confirm_Pulverizador", name = "Confirmación Pulverizador", address = 2, dataType = ModbusDataType.BITMASK_16, bitIndex = 7)
        val Damper = ModbusVariable(id = 208, tag = "Confirm_Damper", name = "Confirmación Damper", address = 2, dataType = ModbusDataType.BITMASK_16, bitIndex = 8)
        val Calefaccion = ModbusVariable(id = 209, tag = "Confirm_Calefaccion", name = "Confirmación Calefacción", address = 2, dataType = ModbusDataType.BITMASK_16, bitIndex = 9)

        val all = listOf(Zona1, Zona2, Zona3, Zona4, Zona5, Zona6, Pulverizador, Damper, Calefaccion)
    }

    // =========================================================================
    // LOOKUP INDEXES
    // =========================================================================
    private val allVariables: List<ModbusVariable> by lazy {
        Commands.all + Telemetry.all + Parameters.all + Alarms.all + Confirmations.all
    }

    private val byTagNormalized: Map<String, ModbusVariable> by lazy {
        allVariables.associateBy { it.tag.trim().uppercase() }
    }

    private val byId: Map<Int, ModbusVariable> by lazy {
        allVariables.associateBy { it.id }
    }

    /**
     * Resolves a parameter by tag name or numeric ID string.
     */
    fun findParameterByTag(tagOrId: String): ModbusVariable? {
        val clean = tagOrId.trim()
        val numericId = clean.toIntOrNull()
        if (numericId != null) {
            val byParameterId = Parameters.all.firstOrNull { it.id == numericId }
            if (byParameterId != null) return byParameterId
        }

        return byTagNormalized[clean.uppercase()]
            ?: Parameters.all.firstOrNull { it.name.equals(clean, ignoreCase = true) }
    }

    /**
     * Resolves any registered variable by its unique ID.
     */
    fun findById(id: Int): ModbusVariable? = byId[id]
}

