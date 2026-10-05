package com.pronavicola.camion.data.modbus

import com.pronavicola.camion.domain.model.ZoneStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * High-fidelity PLC Micro820 simulation client.
 * Reproduces the PLC ladder control sequence:
 * - Timed staggered zone startup (7s, 14s, 21s, 27s, 35s, 42s).
 * - Closed-loop spray/irrigator cycles based on humidity and configured ON/OFF timers.
 * - Closed-loop heating/damper cycles based on temperature thresholds and ON/OFF timers.
 * - Dynamic parameter adjustment matching PLC holding registers.
 */
class SimulatedModbusClient(
    private val scope: CoroutineScope,
) : ModbusClient {
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

    private var pollJob: Job? = null
    private val parameterValues = mutableMapOf<String, Double>()

    // Simulation sequence state
    private var elapsedSeconds = 0
    private var irrigatorTimer = 0
    private var irrigatorIsOn = false
    private var damperTimer = 0
    private var damperIsOn = false

    private var currentHost: String = ModbusClient.DEFAULT_HOST
    private var currentPort: Int = ModbusClient.DEFAULT_PORT

    override suspend fun connect(host: String, port: Int) {
        disconnect()
        this.currentHost = host
        // Modbus TCP industry standard port is always 502
        this.currentPort = ModbusClient.DEFAULT_PORT
        delay(350)
        _connected.value = true
        pollJob = scope.launch(Dispatchers.Default) {
            while (isActive && _connected.value) {
                if (_procesoActivo.value) {
                    elapsedSeconds++

                    // Drift sensor readings
                    _temperatura1.value = drift(_temperatura1.value ?: 14.5, 11.0, 19.0)
                    _temperatura2.value = drift(_temperatura2.value ?: 14.0, 11.0, 19.0)
                    _humedad.value = drift(_humedad.value ?: 62.0, 45.0, 85.0)
                    _tension.value = drift(_tension.value ?: 24.0, 22.0, 26.0)

                    val t1 = _temperatura1.value ?: 14.5
                    val hum = _humedad.value ?: 62.0

                    // Configured / default parameter limits
                    val tempMin = getParam("TempMin", "1", 12.0)
                    val tempMax = getParam("TempMax", "2", 18.0)
                    val humMin = getParam("HumMin", "3", 55.0)
                    val damperTimeOff = getParam("DamperTiempoOff", "5", 30.0).toInt().coerceAtLeast(1)
                    val damperTimeOn = getParam("DamperTiempoOn", "6", 15.0).toInt().coerceAtLeast(1)
                    val pulvTimeOff = getParam("PulverizadorTiempoOff", "7", 20.0).toInt().coerceAtLeast(1)
                    val pulvTimeOn = getParam("PulverizadorTiempoOn", "8", 10.0).toInt().coerceAtLeast(1)

                    // 1. Timed Staggered Startup of Zones
                    val z1 = elapsedSeconds >= 7
                    val z2 = elapsedSeconds >= 14 && (t1 < tempMin || t1 > tempMax)
                    val z3 = elapsedSeconds >= 21
                    val z4 = elapsedSeconds >= 27
                    val z5 = elapsedSeconds >= 35 && t1 > tempMax
                    val z6 = elapsedSeconds >= 42

                    _zonas.value = listOf(
                        if (z1) ZoneStatus.ON else ZoneStatus.OFF,
                        if (z2) ZoneStatus.ON else ZoneStatus.OFF,
                        if (z3) ZoneStatus.ON else ZoneStatus.OFF,
                        if (z4) ZoneStatus.ON else ZoneStatus.OFF,
                        if (z5) ZoneStatus.ON else ZoneStatus.OFF,
                        if (z6) ZoneStatus.ON else ZoneStatus.OFF,
                    )

                    // 2. Closed-Loop Irrigator / Pulverizer Cycle
                    if (hum < humMin) {
                        irrigatorTimer++
                        if (irrigatorIsOn) {
                            if (irrigatorTimer >= pulvTimeOn) {
                                irrigatorIsOn = false
                                irrigatorTimer = 0
                            }
                        } else {
                            if (irrigatorTimer >= pulvTimeOff) {
                                irrigatorIsOn = true
                                irrigatorTimer = 0
                            }
                        }
                        _estadoIrrigador.value = if (irrigatorIsOn) ZoneStatus.ON else ZoneStatus.OFF
                    } else {
                        irrigatorTimer = 0
                        irrigatorIsOn = false
                        _estadoIrrigador.value = ZoneStatus.OFF
                    }

                    // 3. Closed-Loop Heating & Damper Cycle
                    if (t1 < tempMin) {
                        _calefaccionActiva.value = true
                        damperTimer++
                        if (damperIsOn) {
                            if (damperTimer >= damperTimeOn) {
                                damperIsOn = false
                                damperTimer = 0
                            }
                        } else {
                            if (damperTimer >= damperTimeOff) {
                                damperIsOn = true
                                damperTimer = 0
                            }
                        }
                        _damperActivo.value = damperIsOn
                    } else if (t1 > tempMax) {
                        _calefaccionActiva.value = false
                        _damperActivo.value = true
                        damperTimer = 0
                        damperIsOn = true
                    } else {
                        _calefaccionActiva.value = false
                        _damperActivo.value = false
                        damperTimer = 0
                        damperIsOn = false
                    }

                    _alarmasActivas.value = emptyList()
                } else {
                    resetProcessState()
                }
                delay(1000) // 1 second simulation clock
            }
        }
    }

    override suspend fun disconnect() {
        pollJob?.cancel()
        pollJob = null
        _connected.value = false
        resetProcessState()
    }

    override suspend fun iniciarProceso() {
        if (!_connected.value) return
        _procesoActivo.value = true
        elapsedSeconds = 0
        irrigatorTimer = 0
        irrigatorIsOn = false
        damperTimer = 0
        damperIsOn = false

        _temperatura1.value = 14.5
        _temperatura2.value = 14.0
        _humedad.value = 62.0
        _tension.value = 24.0
        _zonas.value = List(6) { ZoneStatus.OFF }
        _estadoIrrigador.value = ZoneStatus.OFF
        _damperActivo.value = false
        _calefaccionActiva.value = false
        _alarmasActivas.value = emptyList()
    }

    override suspend fun detenerProceso() {
        _procesoActivo.value = false
        resetProcessState()
    }

    private fun resetProcessState() {
        elapsedSeconds = 0
        irrigatorTimer = 0
        irrigatorIsOn = false
        damperTimer = 0
        damperIsOn = false
        _temperatura1.value = null
        _temperatura2.value = null
        _humedad.value = null
        _tension.value = null
        _zonas.value = List(6) { ZoneStatus.OFF }
        _estadoIrrigador.value = ZoneStatus.OFF
        _damperActivo.value = false
        _calefaccionActiva.value = false
        _alarmasActivas.value = emptyList()
    }

    override suspend fun writeParameter(tag: String, value: Double) {
        parameterValues[tag.trim().uppercase()] = value
        parameterValues[tag.trim()] = value
    }

    private fun getParam(tag1: String, tag2: String, default: Double): Double {
        return parameterValues[tag1.uppercase()]
            ?: parameterValues[tag1]
            ?: parameterValues[tag2]
            ?: default
    }

    private fun drift(current: Double, min: Double, max: Double): Double {
        val next = current + Random.nextDouble(-0.3, 0.3)
        return next.coerceIn(min, max)
    }
}
