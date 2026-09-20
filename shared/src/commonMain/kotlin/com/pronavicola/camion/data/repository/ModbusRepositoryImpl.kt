package com.pronavicola.camion.data.repository

import com.pronavicola.camion.data.modbus.ModbusClient
import com.pronavicola.camion.domain.model.ZoneStatus
import com.pronavicola.camion.domain.repository.ModbusRepository
import kotlinx.coroutines.flow.StateFlow

class ModbusRepositoryImpl(
    private val modbusClient: ModbusClient,
) : ModbusRepository {
    override val isConnected: StateFlow<Boolean> = modbusClient.isConnected
    override val temperatura1: StateFlow<Double?> = modbusClient.temperatura1
    override val temperatura2: StateFlow<Double?> = modbusClient.temperatura2
    override val humedad: StateFlow<Double?> = modbusClient.humedad
    override val tension: StateFlow<Double?> = modbusClient.tension
    override val procesoActivo: StateFlow<Boolean> = modbusClient.procesoActivo
    override val zonas: StateFlow<List<ZoneStatus>> = modbusClient.zonas
    override val estadoIrrigador: StateFlow<ZoneStatus> = modbusClient.estadoIrrigador
    override val damperActivo: StateFlow<Boolean> = modbusClient.damperActivo
    override val calefaccionActiva: StateFlow<Boolean> = modbusClient.calefaccionActiva
    override val alarmasActivas: StateFlow<List<String>> = modbusClient.alarmasActivas

    override suspend fun connect(host: String, port: Int) = modbusClient.connect(host, port)
    override suspend fun disconnect() = modbusClient.disconnect()
    override suspend fun iniciarProceso() = modbusClient.iniciarProceso()
    override suspend fun detenerProceso() = modbusClient.detenerProceso()
    override suspend fun writeParameter(tag: String, value: Double) = modbusClient.writeParameter(tag, value)
}
