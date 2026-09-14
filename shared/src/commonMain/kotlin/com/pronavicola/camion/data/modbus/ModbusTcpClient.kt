package com.pronavicola.camion.data.modbus

import com.pronavicola.camion.util.Log
import com.pronavicola.camion.domain.model.Micro820ModbusConstants
import com.pronavicola.camion.domain.model.ZoneStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.random.Random

/**
 * Robust Modbus TCP Client communicating directly with Allen-Bradley Micro820 PLC
 * or Modbus TCP gateway over Ethernet/Wi-Fi.
 * Supports Holding Registers (FC 03) reading for analog telemetry, confirmation & alarm bitmasks,
 * pulse commands (FC 05) on Coils 1 & 3, and IEEE-754 Big-Endian parameter writes.
 */
class ModbusTcpClient(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val enableSimulationFallback: Boolean = true,
) : ModbusClient {

    private val tag = "ModbusTcpClient"

    private val _connected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _temperatura1 = MutableStateFlow<Double?>(null)
    override val temperatura1: StateFlow<Double?> = _temperatura1.asStateFlow()

    private val _temperatura2 = MutableStateFlow<Double?>(null)
    override val temperatura2: StateFlow<Double?> = _temperatura2.asStateFlow()

    private val _humedad = MutableStateFlow<Double?>(null)
    override val humedad: StateFlow<Double?> = _humedad.asStateFlow()

    private val _tension = MutableStateFlow<Double?>(null)
    override val tension: StateFlow<Double?> = _tension.asStateFlow()

    private val _procesoActivo = MutableStateFlow(false)
    override val procesoActivo: StateFlow<Boolean> = _procesoActivo.asStateFlow()

    private val _zonas = MutableStateFlow(List(6) { ZoneStatus.OFF })
    override val zonas: StateFlow<List<ZoneStatus>> = _zonas.asStateFlow()

    private val _estadoIrrigador = MutableStateFlow(ZoneStatus.OFF)
    override val estadoIrrigador: StateFlow<ZoneStatus> = _estadoIrrigador.asStateFlow()

    private val _damperActivo = MutableStateFlow(false)
    override val damperActivo: StateFlow<Boolean> = _damperActivo.asStateFlow()

    private val _calefaccionActiva = MutableStateFlow(false)
    override val calefaccionActiva: StateFlow<Boolean> = _calefaccionActiva.asStateFlow()

    private val _alarmasActivas = MutableStateFlow<List<String>>(emptyList())
    override val alarmasActivas: StateFlow<List<String>> = _alarmasActivas.asStateFlow()

    private var socket: Socket? = null
    private var currentHost: String = ModbusClient.DEFAULT_HOST
    private var currentPort: Int = ModbusClient.DEFAULT_PORT
    private var pollJob: Job? = null
    private val socketMutex = Mutex()

    private val parameterCache = mutableMapOf<String, Double>()

    companion object {
        const val SOCKET_TIMEOUT_MS = 5000
        const val POLLING_INTERVAL_MS = 1500L
        const val UNIT_ID = 1
    }

    override suspend fun connect(host: String, port: Int) {
        currentHost = host
        currentPort = port
        disconnect()

        pollJob = scope.launch(ioDispatcher) {
            startPollingLoop()
        }
    }

    override suspend fun disconnect() {
        pollJob?.cancel()
        pollJob = null
        closeSocket()
        _connected.value = false
    }

    private suspend fun startPollingLoop() {
        var consecutiveFailures = 0

        while (scope.isActive) {
            val connectedToPlc = ensureSocketConnected()

            if (connectedToPlc) {
                _connected.value = true
                val pollSuccess = pollPlcData()

                if (pollSuccess) {
                    consecutiveFailures = 0
                } else {
                    consecutiveFailures++
                    Log.w(tag, "Modbus poll failed ($consecutiveFailures consecutive)")
                    if (consecutiveFailures >= 3) {
                        closeSocket()
                        _connected.value = false
                        applyExponentialBackoff(consecutiveFailures)
                    }
                }
            } else {
                _connected.value = false
                delay(3000L) // Retry connection periodically
            }

            delay(POLLING_INTERVAL_MS)
        }
    }

    private suspend fun ensureSocketConnected(): Boolean = socketMutex.withLock {
        try {
            val current = socket
            if (current != null && current.isConnected && !current.isClosed && !current.isInputShutdown) {
                return true
            }

            closeSocketInternal()
            val newSocket = Socket()
            newSocket.soTimeout = SOCKET_TIMEOUT_MS
            newSocket.connect(InetSocketAddress(currentHost, currentPort), SOCKET_TIMEOUT_MS)
            socket = newSocket
            Log.i(tag, "Connected to Modbus TCP server at $currentHost:$currentPort")
            true
        } catch (e: Exception) {
            Log.d(tag, "Could not establish TCP connection to $currentHost:$currentPort: ${e.message}")
            closeSocketInternal()
            false
        }
    }

    private suspend fun closeSocket() = socketMutex.withLock {
        closeSocketInternal()
    }

    private fun closeSocketInternal() {
        try {
            socket?.close()
        } catch (_: Exception) {
        } finally {
            socket = null
        }
    }

    private suspend fun applyExponentialBackoff(failureCount: Int) {
        val delayMs = when (failureCount) {
            1 -> 1000L
            2 -> 2000L
            else -> 4000L
        }
        delay(delayMs)
    }

    /**
     * Polls Holding Registers (FC 03) and Coils (FC 01) from the PLC.
     */
    private suspend fun pollPlcData(): Boolean = socketMutex.withLock {
        val current = socket ?: return false
        try {
            val inStream = current.getInputStream()
            val outStream = current.getOutputStream()

            // 1. Read Holding Registers (40001..40012) -> ALARMA, CONFIRMAR, SALIDAS, HUM, T1, T2, TENSION
            val fc03Req = ModbusProtocol.buildReadHoldingRegistersRequest(
                unitId = UNIT_ID,
                startAddress = Micro820ModbusConstants.REG_ALARMAS,
                quantity = Micro820ModbusConstants.COUNT_HOLDING_REGS,
            )
            outStream.write(fc03Req)
            outStream.flush()

            val fc03Resp = ModbusProtocol.readResponseFrame(inStream)
            val holdingRegisters = ModbusProtocol.parseReadRegistersResponse(fc03Resp, 0x03)

            if (holdingRegisters.size >= Micro820ModbusConstants.COUNT_HOLDING_REGS) {
                val alarmaWord = holdingRegisters[Micro820ModbusConstants.REG_ALARMAS]
                val confirmarWord = holdingRegisters[Micro820ModbusConstants.REG_CONFIRMAR]

                // Real Float telemetry (IEEE-754 Big-Endian, 2 words each)
                val hum = IEEE754Decoder.decodeIEEE754BigEndian(
                    holdingRegisters[Micro820ModbusConstants.REG_HUMEDAD],
                    holdingRegisters[Micro820ModbusConstants.REG_HUMEDAD + 1]
                ).toDouble()

                val t1 = IEEE754Decoder.decodeIEEE754BigEndian(
                    holdingRegisters[Micro820ModbusConstants.REG_TEMPERATURA1],
                    holdingRegisters[Micro820ModbusConstants.REG_TEMPERATURA1 + 1]
                ).toDouble()

                val t2 = IEEE754Decoder.decodeIEEE754BigEndian(
                    holdingRegisters[Micro820ModbusConstants.REG_TEMPERATURA2],
                    holdingRegisters[Micro820ModbusConstants.REG_TEMPERATURA2 + 1]
                ).toDouble()

                val vlt = IEEE754Decoder.decodeIEEE754BigEndian(
                    holdingRegisters[Micro820ModbusConstants.REG_TENSION],
                    holdingRegisters[Micro820ModbusConstants.REG_TENSION + 1]
                ).toDouble()

                _temperatura1.value = if (t1 in -50.0..100.0) t1 else _temperatura1.value
                _temperatura2.value = if (t2 in -50.0..100.0) t2 else _temperatura2.value
                _humedad.value = if (hum in 0.0..100.0) hum else _humedad.value
                _tension.value = if (vlt in 0.0..500.0) vlt else _tension.value

                // Decode Alarmas list from ALARMA word (40001)
                val alarmsList = mutableListOf<String>()
                if ((alarmaWord and (1 shl Micro820ModbusConstants.BIT_ALARM_FALLA_ENERGIA)) != 0) alarmsList.add("Falla de energía")
                if ((alarmaWord and (1 shl Micro820ModbusConstants.BIT_ALARM_ZONA_1)) != 0) alarmsList.add("Falla Zona 1")
                if ((alarmaWord and (1 shl Micro820ModbusConstants.BIT_ALARM_ZONA_2)) != 0) alarmsList.add("Falla Zona 2")
                if ((alarmaWord and (1 shl Micro820ModbusConstants.BIT_ALARM_ZONA_3)) != 0) alarmsList.add("Falla Zona 3")
                if ((alarmaWord and (1 shl Micro820ModbusConstants.BIT_ALARM_ZONA_4)) != 0) alarmsList.add("Falla Zona 4")
                if ((alarmaWord and (1 shl Micro820ModbusConstants.BIT_ALARM_ZONA_5)) != 0) alarmsList.add("Falla Zona 5")
                if ((alarmaWord and (1 shl Micro820ModbusConstants.BIT_ALARM_ZONA_6)) != 0) alarmsList.add("Falla Zona 6")
                if ((alarmaWord and (1 shl Micro820ModbusConstants.BIT_ALARM_PULVERIZADOR)) != 0) alarmsList.add("Falla Pulverizador")
                if ((alarmaWord and (1 shl Micro820ModbusConstants.BIT_ALARM_DAMPER)) != 0) alarmsList.add("Falla Damper")
                if ((alarmaWord and (1 shl Micro820ModbusConstants.BIT_ALARM_CALEFACCION)) != 0) alarmsList.add("Falla Calefacción")
                if ((alarmaWord and (1 shl Micro820ModbusConstants.BIT_ALARM_BAJA_TENSION)) != 0) alarmsList.add("Baja Tensión (< 22V)")
                _alarmasActivas.value = alarmsList

                // Decode Zones 1..6 status (Bit 1..6 in CONFIRMAR / ALARMA)
                _zonas.value = List(6) { index ->
                    val bitPos = index + 1
                    val isFail = (alarmaWord and (1 shl bitPos)) != 0
                    val isConfirmed = (confirmarWord and (1 shl bitPos)) != 0
                    when {
                        isFail -> ZoneStatus.FAIL
                        isConfirmed -> ZoneStatus.ON
                        else -> ZoneStatus.OFF
                    }
                }

                // Decode Pulverizador / Irrigador (Bit 7)
                val irrigatorFail = (alarmaWord and (1 shl Micro820ModbusConstants.BIT_CONFIRM_PULVERIZADOR)) != 0
                val irrigatorOn = (confirmarWord and (1 shl Micro820ModbusConstants.BIT_CONFIRM_PULVERIZADOR)) != 0
                _estadoIrrigador.value = when {
                    irrigatorFail -> ZoneStatus.FAIL
                    irrigatorOn -> ZoneStatus.ON
                    else -> ZoneStatus.OFF
                }

                // Decode Damper (Bit 8) and Calefacción (Bit 9)
                _damperActivo.value = (confirmarWord and (1 shl Micro820ModbusConstants.BIT_CONFIRM_DAMPER)) != 0
                _calefaccionActiva.value = (confirmarWord and (1 shl Micro820ModbusConstants.BIT_CONFIRM_CALEFACCION)) != 0
            }

            // 2. Read Coils (Coil 1: INICIO, Coil 2: EN_PROCESO, Coil 3: DETENER)
            val fc01Req = ModbusProtocol.buildReadCoilsRequest(
                unitId = UNIT_ID,
                startAddress = Micro820ModbusConstants.COIL_INICIO,
                quantity = Micro820ModbusConstants.COUNT_COILS,
            )
            outStream.write(fc01Req)
            outStream.flush()

            val fc01Resp = ModbusProtocol.readResponseFrame(inStream)
            val coils = ModbusProtocol.parseReadCoilsResponse(fc01Resp, Micro820ModbusConstants.COUNT_COILS)

            if (coils.size >= 2) {
                _procesoActivo.value = coils[Micro820ModbusConstants.COIL_EN_PROCESO]
            }

            true
        } catch (e: Exception) {
            Log.w(tag, "Modbus read transaction error: ${e.message}")
            closeSocketInternal()
            false
        }
    }

    override suspend fun iniciarProceso() {
        _procesoActivo.value = true
        // Write pulse to Coil 1 (INICIO)
        writeCoilToPlc(coilAddress = Micro820ModbusConstants.COIL_INICIO, state = true)
    }

    override suspend fun detenerProceso() {
        _procesoActivo.value = false
        _zonas.value = List(6) { ZoneStatus.OFF }
        _estadoIrrigador.value = ZoneStatus.OFF
        _damperActivo.value = false
        _calefaccionActiva.value = false
        // Write pulse to Coil 3 (DETENER)
        writeCoilToPlc(coilAddress = Micro820ModbusConstants.COIL_DETENER, state = true)
    }

    private suspend fun writeCoilToPlc(coilAddress: Int, state: Boolean) = withContext(ioDispatcher) {
        socketMutex.withLock {
            val current = socket ?: return@withLock
            try {
                val req = ModbusProtocol.buildWriteSingleCoilRequest(
                    unitId = UNIT_ID,
                    coilAddress = coilAddress,
                    state = state,
                )
                val out = current.getOutputStream()
                out.write(req)
                out.flush()
                val resp = ModbusProtocol.readResponseFrame(current.getInputStream())
                ModbusProtocol.checkErrorResponse(resp)
            } catch (e: Exception) {
                Log.w(tag, "Write coil $coilAddress failed: ${e.message}")
            }
        }
    }

    override suspend fun writeParameter(tag: String, value: Double): Unit = withContext(ioDispatcher) {
        parameterCache[tag] = value
        val registerAddress = resolveRegisterAddress(tag)

        socketMutex.withLock {
            val current = socket
            if (current == null || !current.isConnected) {
                Log.d(this@ModbusTcpClient.tag, "Cached parameter $tag = $value (PLC offline)")
                return@withLock
            }

            try {
                val inStream = current.getInputStream()
                val outStream = current.getOutputStream()

                // Encode as REAL IEEE-754 float (2 words)
                val words = IEEE754Decoder.encodeIEEE754BigEndian(value.toFloat())
                val writeReq = ModbusProtocol.buildWriteMultipleRegistersRequest(
                    unitId = UNIT_ID,
                    startAddress = registerAddress,
                    values = listOf(words.first, words.second),
                )
                outStream.write(writeReq)
                outStream.flush()

                val writeResp = ModbusProtocol.readResponseFrame(inStream)
                ModbusProtocol.checkErrorResponse(writeResp)

                // Echo-Read: read back holding registers to verify acceptance
                val echoReq = ModbusProtocol.buildReadHoldingRegistersRequest(
                    unitId = UNIT_ID,
                    startAddress = registerAddress,
                    quantity = 2,
                )
                outStream.write(echoReq)
                outStream.flush()

                val echoResp = ModbusProtocol.readResponseFrame(inStream)
                val readWords = ModbusProtocol.parseReadRegistersResponse(echoResp, 0x03)
                if (readWords.size >= 2) {
                    val confirmed = IEEE754Decoder.decodeIEEE754BigEndian(readWords[0], readWords[1])
                    Log.i(this@ModbusTcpClient.tag, "Echo-Read confirmed parameter $tag = $confirmed (wrote $value)")
                }
            } catch (e: Exception) {
                Log.w(this@ModbusTcpClient.tag, "Modbus writeParameter failed for $tag: ${e.message}")
            }
        }
    }

    /**
     * Maps tag names or IDs to PLC holding register addresses (40001 + offset).
     */
    private fun resolveRegisterAddress(tag: String): Int {
        val cleanTag = tag.trim().uppercase()
        return when (cleanTag) {
            "TEMPMIN", "1" -> Micro820ModbusConstants.REG_PARAM_TEMP_MIN
            "TEMPMAX", "2" -> Micro820ModbusConstants.REG_PARAM_TEMP_MAX
            "HUMMIN", "3" -> Micro820ModbusConstants.REG_PARAM_HUM_MIN
            "HUMMAX", "4" -> Micro820ModbusConstants.REG_PARAM_HUM_MAX
            "DAMPERTIEMPOOFF", "5" -> Micro820ModbusConstants.REG_PARAM_DAMPER_TIEMPO_OFF
            "DAMPERTIEMPOON", "6" -> Micro820ModbusConstants.REG_PARAM_DAMPER_TIEMPO_ON
            "PULVERIZADORTIEMPOOFF", "7" -> Micro820ModbusConstants.REG_PARAM_PULV_TIEMPO_OFF
            "PULVERIZADORTIEMPOON", "8" -> Micro820ModbusConstants.REG_PARAM_PULV_TIEMPO_ON
            else -> {
                val numericPart = cleanTag.filter { it.isDigit() }.toIntOrNull()
                if (numericPart != null && numericPart > 0) {
                    (numericPart - 1) * 2 + Micro820ModbusConstants.REG_PARAM_TEMP_MIN
                } else {
                    Micro820ModbusConstants.REG_PARAM_TEMP_MIN
                }
            }
        }
    }
}
