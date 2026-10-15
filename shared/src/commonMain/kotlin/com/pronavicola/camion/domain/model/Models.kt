package com.pronavicola.camion.domain.model

data class SessionUser(
    val idUsuario: String,
    val usuario: String,
    val idPerfil: Int,
    val idVehiculo: Int = 1,
)

data class ParametroUiModel(
    val idParametro: Int,
    val nombre: String,
    val valor: Double,
    val unidad: String,
    val minimo: Double,
    val maximo: Double,
    val esDecimal: Boolean,
    val editable: Boolean,
    val tag: String,
)

enum class ProcesoEstado(val label: String) {
    INACTIVO("DETENIDO"),
    ACTIVO("EN PROCESO"),
}

enum class ZoneStatus {
    OFF,
    ON,
    FAIL
}

data class HmiState(
    val idVehiculo: String = "AAA123",
    val conectado: Boolean = false,
    val temperatura1: Double? = null,
    val temperatura2: Double? = null,
    val humedad: Double? = null,
    val tension: Double? = null,
    val procesoEstado: ProcesoEstado = ProcesoEstado.INACTIVO,
    val zonas: List<ZoneStatus> = List(6) { ZoneStatus.OFF },
    val estadoIrrigador: ZoneStatus = ZoneStatus.OFF,
    val damperActivo: Boolean = false,
    val calefaccionActiva: Boolean = false,
    val alarmasActivas: List<String> = emptyList(),
    val history: List<HmiHistoryPoint> = emptyList(),
    val mensaje: String? = null,
)

object Micro820ModbusConstants {
    // Coils (0-based offsets for PDU)
    const val COIL_INICIO = 0      // Coil 1: INICIO (Pulse FC 05)
    const val COIL_EN_PROCESO = 1  // Coil 2: EN_PROCESO (Read FC 01)
    const val COIL_DETENER = 2     // Coil 3: DETENER (Pulse FC 05)
    const val COUNT_COILS = 3

    // Holding Registers (0-based offsets for PDU: 40001 + offset)
    const val REG_ALARMAS = 0      // 40001: ALARMAS (Word bitmask)
    const val REG_CONFIRMAR = 1    // 40002: CONFIRMAR (Word bitmask)
    const val REG_SALIDAS = 2      // 40003: SALIDAS (Word bitmask)
    const val REG_HUMEDAD = 4      // 40005: HUMEDAD (REAL 2 words: 40005, 40006)
    const val REG_TEMPERATURA1 = 6 // 40007: TEMP 1 (REAL 2 words: 40007, 40008)
    const val REG_TEMPERATURA2 = 8 // 40009: TEMP 2 (REAL 2 words: 40009, 40010)
    const val REG_TENSION = 10     // 40011: TENSION (REAL 2 words: 40011, 40012)
    const val COUNT_HOLDING_REGS = 12

    // Holding Registers - Parámetros (40029..40043)
    const val REG_PARAM_TEMP_MIN = 28               // 40029 (REAL 2 words)
    const val REG_PARAM_TEMP_MAX = 30               // 40031 (REAL 2 words)
    const val REG_PARAM_HUM_MIN = 32                // 40033 (REAL 2 words)
    const val REG_PARAM_HUM_MAX = 34                // 40035 (REAL 2 words)
    const val REG_PARAM_DAMPER_TIEMPO_OFF = 36      // 40037 (REAL 2 words)
    const val REG_PARAM_DAMPER_TIEMPO_ON = 38       // 40039 (REAL 2 words)
    const val REG_PARAM_PULV_TIEMPO_OFF = 40        // 40041 (REAL 2 words)
    const val REG_PARAM_PULV_TIEMPO_ON = 42         // 40043 (REAL 2 words)

    // Bit positions in REG_CONFIRMAR (40002)
    const val BIT_CONFIRM_ZONA_1 = 1
    const val BIT_CONFIRM_ZONA_2 = 2
    const val BIT_CONFIRM_ZONA_3 = 3
    const val BIT_CONFIRM_ZONA_4 = 4
    const val BIT_CONFIRM_ZONA_5 = 5
    const val BIT_CONFIRM_ZONA_6 = 6
    const val BIT_CONFIRM_PULVERIZADOR = 7
    const val BIT_CONFIRM_DAMPER = 8
    const val BIT_CONFIRM_CALEFACCION = 9

    // Bit positions in REG_ALARMAS (40001) - Matches IDs 1..11 in Alarmas table
    const val BIT_ALARM_FALLA_ENERGIA = 0      // ID 1: falla de energia
    const val BIT_ALARM_ZONA_1 = 1             // ID 2: zona 1 no enciende
    const val BIT_ALARM_ZONA_2 = 2             // ID 3: zona 2 no enciende
    const val BIT_ALARM_ZONA_3 = 3             // ID 4: zona 3 no enciende
    const val BIT_ALARM_ZONA_4 = 4             // ID 5: zona 4 no enciende
    const val BIT_ALARM_ZONA_5 = 5             // ID 6: zona 5 no enciende
    const val BIT_ALARM_ZONA_6 = 6             // ID 7: zona 6 no enciende
    const val BIT_ALARM_PULVERIZADOR = 7       // ID 8: no humifica
    const val BIT_ALARM_DAMPER = 8             // ID 9: damper no abre
    const val BIT_ALARM_CALEFACCION = 9        // ID 10: calefaccion no enciende
    const val BIT_ALARM_NIVEL_AGUA_BAJO = 10   // ID 11: nivel de agua bajo
    const val BIT_ALARM_BAJA_TENSION = 10      // Alias for backwards compatibility
}

data class HmiHistoryPoint(
    val t1: Double,
    val t2: Double,
    val hum: Double,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AppDestination(val route: String, val label: String, val accessLevel: Int, val showInNavBar: Boolean = true) {
    HMI("hmi", "CAMION", 4, showInNavBar = true),
    REGISTROS("registros", "REGISTROS", 4, showInNavBar = true),
    PARAMETROS("parametros", "PARAM", 4, showInNavBar = true),
    ALARMAS_REGISTROS("alarmas_registros", "ALARMAS", 4, showInNavBar = true),
    CONFIG("config", "CONFIG", 0, showInNavBar = true),
    SIMULACION("simulacion", "SIMULADOR", 0, showInNavBar = false),
}

data class MqttLogEvent(
    val timestamp: String,
    val level: String,
    val message: String,
)

