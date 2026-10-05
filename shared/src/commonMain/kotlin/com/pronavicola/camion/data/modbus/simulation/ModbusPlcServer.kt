package com.pronavicola.camion.data.modbus.simulation

import com.pronavicola.camion.data.modbus.IEEE754Decoder
import com.pronavicola.camion.domain.model.Micro820ModbusConstants
import com.pronavicola.camion.domain.model.SimulationConfig
import com.pronavicola.camion.domain.model.SimulationState
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.random.Random

class ModbusPlcServer(
    private val scope: CoroutineScope,
    initialConfig: SimulationConfig = SimulationConfig(),
) {
    private val tag = "ModbusPlcServer"
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var ladderJob: Job? = null

    private val _state = MutableStateFlow(SimulationState(config = initialConfig, port = initialConfig.port))
    val state: StateFlow<SimulationState> = _state.asStateFlow()

    // PLC Memory
    private val coils = BooleanArray(2000)
    private val holdingRegisters = ShortArray(2000)

    // Ladder Logic State
    private var isRunning = false
    private var simTickCount = 0
    private var injectedAlarmsMask = 0

    init {
        applyConfig(initialConfig)
    }

    fun applyConfig(newConfig: SimulationConfig) {
        _state.update { it.copy(config = newConfig, port = newConfig.port) }
        writeHoldingRegisterFloat(Micro820ModbusConstants.REG_PARAM_TEMP_MIN, 12.0f)
        writeHoldingRegisterFloat(Micro820ModbusConstants.REG_PARAM_TEMP_MAX, 18.0f)
        writeHoldingRegisterFloat(Micro820ModbusConstants.REG_PARAM_HUM_MIN, 55.0f)
        writeHoldingRegisterFloat(Micro820ModbusConstants.REG_PARAM_HUM_MAX, 80.0f)

        writeHoldingRegisterFloat(Micro820ModbusConstants.REG_TEMPERATURA1, newConfig.baselineTemperature1.toFloat())
        writeHoldingRegisterFloat(Micro820ModbusConstants.REG_TEMPERATURA2, newConfig.baselineTemperature2.toFloat())
        writeHoldingRegisterFloat(Micro820ModbusConstants.REG_HUMEDAD, newConfig.baselineHumidity.toFloat())
        writeHoldingRegisterFloat(Micro820ModbusConstants.REG_TENSION, newConfig.baselineVoltage.toFloat())

        if (newConfig.failureEnergy) injectAlarmBit(Micro820ModbusConstants.BIT_ALARM_FALLA_ENERGIA, true)
        if (newConfig.failurePulverizador) injectAlarmBit(Micro820ModbusConstants.BIT_ALARM_PULVERIZADOR, true)
        if (newConfig.failureDamper) injectAlarmBit(Micro820ModbusConstants.BIT_ALARM_DAMPER, true)
        if (newConfig.failureCalefaccion) injectAlarmBit(Micro820ModbusConstants.BIT_ALARM_CALEFACCION, true)
    }

    fun injectAlarmBit(bit: Int, active: Boolean) {
        if (active) {
            injectedAlarmsMask = injectedAlarmsMask or (1 shl bit)
        } else {
            injectedAlarmsMask = injectedAlarmsMask and (1 shl bit).inv()
        }
        holdingRegisters[Micro820ModbusConstants.REG_ALARMAS] = injectedAlarmsMask.toShort()
    }

    fun start() {
        if (serverJob?.isActive == true) return
        val requestedPort = _state.value.config.port

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                var actualPort = requestedPort
                val socket = try {
                    ServerSocket(requestedPort)
                } catch (e: Exception) {
                    if (requestedPort < 1024) {
                        actualPort = 1502
                        Log.w(
                            tag,
                            "Binding to privileged port $requestedPort failed on Android (${e.message}). " +
                                "Falling back to non-privileged port $actualPort for Modbus Simulator Server."
                        )
                        ServerSocket(actualPort)
                    } else {
                        throw e
                    }
                }
                serverSocket = socket
                socket.soTimeout = 1000
                Log.i(tag, "Modbus Simulator Server started on port $actualPort")
                _state.update { it.copy(isRunning = true, port = actualPort) }

                // Start Ladder Logic Engine
                startLadderLogic()

                while (isActive) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        if (clientSocket != null) {
                            Log.d(tag, "Client connected: ${clientSocket.inetAddress}")
                            _state.update { it.copy(clientCount = it.clientCount + 1) }
                            scope.launch(Dispatchers.IO) {
                                try {
                                    handleClient(clientSocket)
                                } finally {
                                    _state.update { it.copy(clientCount = (it.clientCount - 1).coerceAtLeast(0)) }
                                }
                            }
                        }
                    } catch (_: SocketTimeoutException) {
                        // Loop to check isActive
                    } catch (e: Exception) {
                        if (isActive) Log.e(tag, "Server accept error", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to start server on port $requestedPort", e)
            } finally {
                stop()
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        ladderJob?.cancel()
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        _state.update { 
            it.copy(
                isRunning = false, 
                clientCount = 0,
                currentT1 = 0.0,
                currentT2 = 0.0,
                currentHum = 0.0,
                currentVoltage = 0.0
            ) 
        }
        Log.i(tag, "Modbus Simulator Server stopped")
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.soTimeout = 5000
            val input = socket.getInputStream()
            val output = socket.getOutputStream()
            val buffer = ByteArray(1024)

            while (isActive && !socket.isClosed) {
                try {
                    val bytesRead = input.read(buffer)
                    if (bytesRead == -1) break

                    if (bytesRead >= 7) {
                        val transactionId = ((buffer[0].toInt() and 0xFF) shl 8) or (buffer[1].toInt() and 0xFF)
                        val protocolId = ((buffer[2].toInt() and 0xFF) shl 8) or (buffer[3].toInt() and 0xFF)
                        val length = ((buffer[4].toInt() and 0xFF) shl 8) or (buffer[5].toInt() and 0xFF)
                        val unitId = buffer[6].toInt() and 0xFF

                        if (protocolId == 0 && length > 0 && bytesRead >= 6 + length) {
                            val functionCode = buffer[7].toInt() and 0xFF
                            val responsePdu = processModbusRequest(functionCode, buffer, 8)

                            val responseLength = responsePdu.size + 1
                            val responseFrame = ByteArray(6 + responseLength)
                            responseFrame[0] = (transactionId shr 8).toByte()
                            responseFrame[1] = transactionId.toByte()
                            responseFrame[2] = 0
                            responseFrame[3] = 0
                            responseFrame[4] = (responseLength shr 8).toByte()
                            responseFrame[5] = responseLength.toByte()
                            responseFrame[6] = unitId.toByte()
                            System.arraycopy(responsePdu, 0, responseFrame, 7, responsePdu.size)

                            output.write(responseFrame)
                            output.flush()
                        }
                    }
                } catch (_: SocketTimeoutException) {
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "Client connection error: ${e.message}")
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun processModbusRequest(functionCode: Int, requestData: ByteArray, pduOffset: Int): ByteArray {
        return when (functionCode) {
            0x01 -> handleReadCoils(requestData, pduOffset)
            0x03 -> handleReadHoldingRegisters(requestData, pduOffset)
            0x05 -> handleWriteSingleCoil(requestData, pduOffset)
            0x10 -> handleWriteMultipleRegisters(requestData, pduOffset)
            else -> buildExceptionResponse(functionCode, 0x01)
        }
    }

    private fun handleReadCoils(req: ByteArray, offset: Int): ByteArray {
        val startAddress = ((req[offset].toInt() and 0xFF) shl 8) or (req[offset + 1].toInt() and 0xFF)
        val quantity = ((req[offset + 2].toInt() and 0xFF) shl 8) or (req[offset + 3].toInt() and 0xFF)

        if (quantity < 1 || quantity > 2000) return buildExceptionResponse(0x01, 0x03)
        if (startAddress + quantity > coils.size) return buildExceptionResponse(0x01, 0x02)

        val byteCount = (quantity + 7) / 8
        val resp = ByteArray(2 + byteCount)
        resp[0] = 0x01.toByte()
        resp[1] = byteCount.toByte()

        for (i in 0 until quantity) {
            if (coils[startAddress + i]) {
                val byteIndex = 2 + (i / 8)
                val bitIndex = i % 8
                resp[byteIndex] = (resp[byteIndex].toInt() or (1 shl bitIndex)).toByte()
            }
        }
        return resp
    }

    private fun handleReadHoldingRegisters(req: ByteArray, offset: Int): ByteArray {
        val startAddress = ((req[offset].toInt() and 0xFF) shl 8) or (req[offset + 1].toInt() and 0xFF)
        val quantity = ((req[offset + 2].toInt() and 0xFF) shl 8) or (req[offset + 3].toInt() and 0xFF)

        if (quantity < 1 || quantity > 125) return buildExceptionResponse(0x03, 0x03)
        if (startAddress + quantity > holdingRegisters.size) return buildExceptionResponse(0x03, 0x02)

        val byteCount = quantity * 2
        val resp = ByteArray(2 + byteCount)
        resp[0] = 0x03.toByte()
        resp[1] = byteCount.toByte()

        for (i in 0 until quantity) {
            val value = holdingRegisters[startAddress + i].toInt()
            resp[2 + i * 2] = (value shr 8).toByte()
            resp[3 + i * 2] = value.toByte()
        }
        return resp
    }

    private fun handleWriteSingleCoil(req: ByteArray, offset: Int): ByteArray {
        val address = ((req[offset].toInt() and 0xFF) shl 8) or (req[offset + 1].toInt() and 0xFF)
        val value = ((req[offset + 2].toInt() and 0xFF) shl 8) or (req[offset + 3].toInt() and 0xFF)

        if (address >= coils.size) return buildExceptionResponse(0x05, 0x02)

        val coilState = value == 0xFF00
        coils[address] = coilState

        if (address == Micro820ModbusConstants.COIL_INICIO && coilState) {
            isRunning = true
            coils[Micro820ModbusConstants.COIL_EN_PROCESO] = true
            simTickCount = 0
            scope.launch { delay(200); coils[address] = false }
        } else if (address == Micro820ModbusConstants.COIL_DETENER && coilState) {
            isRunning = false
            coils[Micro820ModbusConstants.COIL_EN_PROCESO] = false
            simTickCount = 0
            scope.launch { delay(200); coils[address] = false }
        }

        val resp = ByteArray(5)
        resp[0] = 0x05.toByte()
        resp[1] = (address shr 8).toByte()
        resp[2] = address.toByte()
        resp[3] = (value shr 8).toByte()
        resp[4] = value.toByte()
        return resp
    }

    private fun handleWriteMultipleRegisters(req: ByteArray, offset: Int): ByteArray {
        val startAddress = ((req[offset].toInt() and 0xFF) shl 8) or (req[offset + 1].toInt() and 0xFF)
        val quantity = ((req[offset + 2].toInt() and 0xFF) shl 8) or (req[offset + 3].toInt() and 0xFF)
        val byteCount = req[offset + 4].toInt() and 0xFF

        if (quantity < 1 || quantity > 123 || byteCount != quantity * 2) return buildExceptionResponse(0x10, 0x03)
        if (startAddress + quantity > holdingRegisters.size) return buildExceptionResponse(0x10, 0x02)

        for (i in 0 until quantity) {
            val highByte = req[offset + 5 + i * 2].toInt() and 0xFF
            val lowByte = req[offset + 6 + i * 2].toInt() and 0xFF
            holdingRegisters[startAddress + i] = ((highByte shl 8) or lowByte).toShort()
        }

        val resp = ByteArray(5)
        resp[0] = 0x10.toByte()
        resp[1] = (startAddress shr 8).toByte()
        resp[2] = startAddress.toByte()
        resp[3] = (quantity shr 8).toByte()
        resp[4] = quantity.toByte()
        return resp
    }

    private fun buildExceptionResponse(functionCode: Int, exceptionCode: Int): ByteArray {
        val resp = ByteArray(2)
        resp[0] = (functionCode or 0x80).toByte()
        resp[1] = exceptionCode.toByte()
        return resp
    }

    private fun startLadderLogic() {
        ladderJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(_state.value.config.tickDelayMs.coerceAtLeast(200L))
                executeLadderScan()
            }
        }
    }

    private fun executeLadderScan() {
        val cfg = _state.value.config

        if (!isRunning) {
            writeHoldingRegisterFloat(Micro820ModbusConstants.REG_TEMPERATURA1, cfg.baselineTemperature1.toFloat())
            writeHoldingRegisterFloat(Micro820ModbusConstants.REG_TEMPERATURA2, cfg.baselineTemperature2.toFloat())
            writeHoldingRegisterFloat(Micro820ModbusConstants.REG_HUMEDAD, cfg.baselineHumidity.toFloat())
            writeHoldingRegisterFloat(Micro820ModbusConstants.REG_TENSION, cfg.baselineVoltage.toFloat())
            holdingRegisters[Micro820ModbusConstants.REG_CONFIRMAR] = 0.toShort()
            holdingRegisters[Micro820ModbusConstants.REG_ALARMAS] = injectedAlarmsMask.toShort()
            return
        }

        simTickCount++

        val tempMin = readHoldingRegisterFloat(Micro820ModbusConstants.REG_PARAM_TEMP_MIN) ?: 12.0f
        val tempMax = readHoldingRegisterFloat(Micro820ModbusConstants.REG_PARAM_TEMP_MAX) ?: 18.0f
        val humMin = readHoldingRegisterFloat(Micro820ModbusConstants.REG_PARAM_HUM_MIN) ?: 55.0f

        val amp = cfg.oscillationAmplitude.toFloat()
        val currentT1 = drift(
            readHoldingRegisterFloat(Micro820ModbusConstants.REG_TEMPERATURA1) ?: cfg.baselineTemperature1.toFloat(),
            (cfg.baselineTemperature1 - amp * 3).toFloat(),
            (cfg.baselineTemperature1 + amp * 3).toFloat()
        )
        val currentT2 = drift(
            readHoldingRegisterFloat(Micro820ModbusConstants.REG_TEMPERATURA2) ?: cfg.baselineTemperature2.toFloat(),
            (cfg.baselineTemperature2 - amp * 3).toFloat(),
            (cfg.baselineTemperature2 + amp * 3).toFloat()
        )
        val currentHum = drift(
            readHoldingRegisterFloat(Micro820ModbusConstants.REG_HUMEDAD) ?: cfg.baselineHumidity.toFloat(),
            40.0f,
            90.0f
        )
        val currentV = drift(
            readHoldingRegisterFloat(Micro820ModbusConstants.REG_TENSION) ?: cfg.baselineVoltage.toFloat(),
            22.0f,
            26.0f
        )

        writeHoldingRegisterFloat(Micro820ModbusConstants.REG_TEMPERATURA1, currentT1)
        writeHoldingRegisterFloat(Micro820ModbusConstants.REG_TEMPERATURA2, currentT2)
        writeHoldingRegisterFloat(Micro820ModbusConstants.REG_HUMEDAD, currentHum)
        writeHoldingRegisterFloat(Micro820ModbusConstants.REG_TENSION, currentV)

        _state.update {
            it.copy(
                currentT1 = currentT1.toDouble(),
                currentT2 = currentT2.toDouble(),
                currentHum = currentHum.toDouble(),
                currentVoltage = currentV.toDouble()
            )
        }

        val elapsedSec = simTickCount * (cfg.tickDelayMs / 1000.0)
        var confirmarMask = 0

        val z1 = elapsedSec >= 7.0
        val z2 = elapsedSec >= 14.0 && (currentT1 < tempMin || currentT1 > tempMax)
        val z3 = elapsedSec >= 21.0
        val z4 = elapsedSec >= 27.0
        val z5 = elapsedSec >= 35.0 && currentT1 > tempMax
        val z6 = elapsedSec >= 42.0

        if (z1) confirmarMask = confirmarMask or (1 shl 1)
        if (z2) confirmarMask = confirmarMask or (1 shl 2)
        if (z3) confirmarMask = confirmarMask or (1 shl 3)
        if (z4) confirmarMask = confirmarMask or (1 shl 4)
        if (z5) confirmarMask = confirmarMask or (1 shl 5)
        if (z6) confirmarMask = confirmarMask or (1 shl 6)

        val pulverizador = currentHum < humMin && (simTickCount % 12 < 6)
        if (pulverizador) confirmarMask = confirmarMask or (1 shl Micro820ModbusConstants.BIT_CONFIRM_PULVERIZADOR)

        val damper = currentT1 > tempMax || (currentT1 < tempMin && (simTickCount % 16 < 8))
        if (damper) confirmarMask = confirmarMask or (1 shl Micro820ModbusConstants.BIT_CONFIRM_DAMPER)

        val calefaccion = currentT1 < tempMin
        if (calefaccion) confirmarMask = confirmarMask or (1 shl Micro820ModbusConstants.BIT_CONFIRM_CALEFACCION)

        holdingRegisters[Micro820ModbusConstants.REG_CONFIRMAR] = confirmarMask.toShort()
        holdingRegisters[Micro820ModbusConstants.REG_ALARMAS] = injectedAlarmsMask.toShort()
    }

    private fun writeHoldingRegisterFloat(address: Int, value: Float) {
        val words = IEEE754Decoder.encodeIEEE754BigEndian(value)
        holdingRegisters[address] = words.first.toShort()
        holdingRegisters[address + 1] = words.second.toShort()
    }

    private fun readHoldingRegisterFloat(address: Int): Float? {
        val w1 = holdingRegisters[address].toInt()
        val w2 = holdingRegisters[address + 1].toInt()
        return IEEE754Decoder.decodeIEEE754BigEndian(w1, w2)
    }

    private fun drift(current: Float, min: Float, max: Float): Float {
        val next = current + Random.nextFloat() * 0.8f - 0.4f
        return next.coerceIn(min, max)
    }
}
