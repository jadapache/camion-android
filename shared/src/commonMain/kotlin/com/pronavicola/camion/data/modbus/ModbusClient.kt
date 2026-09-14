package com.pronavicola.camion.data.modbus

import com.pronavicola.camion.domain.model.ProcesoEstado
import com.pronavicola.camion.domain.model.ZoneStatus
import kotlinx.coroutines.flow.StateFlow

interface ModbusClient {
    val isConnected: StateFlow<Boolean>
    val temperatura1: StateFlow<Double?>
    val temperatura2: StateFlow<Double?>
    val humedad: StateFlow<Double?>
    val tension: StateFlow<Double?>
    val procesoActivo: StateFlow<Boolean>
    val zonas: StateFlow<List<ZoneStatus>>
    val estadoIrrigador: StateFlow<ZoneStatus>

    suspend fun connect(host: String = DEFAULT_HOST, port: Int = DEFAULT_PORT)
    suspend fun disconnect()
    suspend fun iniciarProceso()
    suspend fun detenerProceso()
    suspend fun writeParameter(tag: String, value: Double)

    companion object {
        const val DEFAULT_HOST = "192.168.1.10"
        const val DEFAULT_PORT = 502
    }
}

fun Boolean.toProcesoEstado(): ProcesoEstado =
    if (this) ProcesoEstado.ACTIVO else ProcesoEstado.INACTIVO
