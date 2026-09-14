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
 * Simulates PLC readings until a real Modbus TCP client is wired in.
 * Values drift within realistic ranges for truck temperature control.
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

    private var pollJob: Job? = null
    private val parameterValues = mutableMapOf<String, Double>()

    override suspend fun connect(host: String, port: Int) {
        disconnect()
        delay(350)
        _connected.value = true
        pollJob = scope.launch(Dispatchers.Default) {
            while (isActive && _connected.value) {
                if (_procesoActivo.value) {
                    _temperatura1.value = drift(_temperatura1.value ?: 14.5, 12.0, 18.0)
                    _temperatura2.value = drift(_temperatura2.value ?: 14.0, 12.0, 18.0)
                    _humedad.value = drift(_humedad.value ?: 62.0, 45.0, 85.0)
                    _tension.value = drift(_tension.value ?: 24.0, 22.0, 26.0)

                    // Simulate some logic for fans and pump
                    _zonas.value = List(6) { index ->
                        val temp = if (index < 3) _temperatura1.value ?: 0.0 else _temperatura2.value ?: 0.0
                        if (temp > 15.0) ZoneStatus.ON else ZoneStatus.OFF
                    }
                    _estadoIrrigador.value = if ((_humedad.value ?: 100.0) < 55.0) ZoneStatus.ON else ZoneStatus.OFF
                } else {
                    _zonas.value = List(6) { ZoneStatus.OFF }
                    _estadoIrrigador.value = ZoneStatus.OFF
                }
                delay(2000)
            }
        }
    }

    override suspend fun disconnect() {
        pollJob?.cancel()
        pollJob = null
        _connected.value = false
    }

    override suspend fun iniciarProceso() {
        if (!_connected.value) return
        _procesoActivo.value = true
        _temperatura1.value = 14.5
        _temperatura2.value = 14.0
        _humedad.value = 62.0
        _tension.value = 24.0
        _zonas.value = List(6) { ZoneStatus.OFF }
        _estadoIrrigador.value = ZoneStatus.OFF
    }

    override suspend fun detenerProceso() {
        _procesoActivo.value = false
    }

    override suspend fun writeParameter(tag: String, value: Double) {
        parameterValues[tag] = value
    }

    private fun drift(current: Double, min: Double, max: Double): Double {
        val next = current + Random.nextDouble(-0.4, 0.4)
        return next.coerceIn(min, max)
    }
}
