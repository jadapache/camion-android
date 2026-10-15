package com.pronavicola.camion.domain.repository

import com.pronavicola.camion.domain.model.ZoneStatus
import kotlinx.coroutines.flow.StateFlow

interface ModbusRepository {
    val isConnected: StateFlow<Boolean>
    val temperatura1: StateFlow<Double?>
    val temperatura2: StateFlow<Double?>
    val humedad: StateFlow<Double?>
    val tension: StateFlow<Double?>
    val procesoActivo: StateFlow<Boolean>
    val zonas: StateFlow<List<ZoneStatus>>
    val estadoIrrigador: StateFlow<ZoneStatus>
    val damperActivo: StateFlow<Boolean>
    val calefaccionActiva: StateFlow<Boolean>
    val alarmasActivas: StateFlow<List<String>>

    suspend fun connect(host: String, port: Int)
    suspend fun disconnect()
    suspend fun iniciarProceso()
    suspend fun detenerProceso()
    suspend fun writeParameter(tag: String, value: Double)
}
