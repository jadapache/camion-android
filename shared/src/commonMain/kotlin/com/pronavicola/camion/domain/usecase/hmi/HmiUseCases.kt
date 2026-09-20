package com.pronavicola.camion.domain.usecase.hmi

import com.pronavicola.camion.domain.model.HmiState
import com.pronavicola.camion.domain.model.ProcesoEstado
import com.pronavicola.camion.domain.repository.ModbusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveHmiStateUseCase(
    private val modbusRepository: ModbusRepository,
) {
    operator fun invoke(deviceId: String): Flow<HmiState> {
        return combine(
            modbusRepository.isConnected,
            modbusRepository.temperatura1,
            modbusRepository.temperatura2,
            modbusRepository.humedad,
            modbusRepository.tension,
            modbusRepository.procesoActivo,
            modbusRepository.zonas,
            modbusRepository.estadoIrrigador,
            modbusRepository.damperActivo,
            modbusRepository.calefaccionActiva,
            modbusRepository.alarmasActivas,
        ) { values ->
            val isConnected = values[0] as Boolean
            val t1 = values[1] as Double?
            val t2 = values[2] as Double?
            val hum = values[3] as Double?
            val tension = values[4] as Double?
            val procesoActivo = values[5] as Boolean
            @Suppress("UNCHECKED_CAST")
            val zonas = values[6] as List<com.pronavicola.camion.domain.model.ZoneStatus>
            val irrigador = values[7] as com.pronavicola.camion.domain.model.ZoneStatus
            val damper = values[8] as Boolean
            val calefaccion = values[9] as Boolean
            @Suppress("UNCHECKED_CAST")
            val alarmas = values[10] as List<String>

            HmiState(
                idVehiculo = deviceId,
                conectado = isConnected,
                temperatura1 = t1,
                temperatura2 = t2,
                humedad = hum,
                tension = tension,
                procesoEstado = if (procesoActivo) ProcesoEstado.ACTIVO else ProcesoEstado.INACTIVO,
                zonas = zonas,
                estadoIrrigador = irrigador,
                damperActivo = damper,
                calefaccionActiva = calefaccion,
                alarmasActivas = alarmas,
            )
        }
    }
}

class StartProcessUseCase(private val modbusRepository: ModbusRepository) {
    suspend operator fun invoke() = modbusRepository.iniciarProceso()
}

class StopProcessUseCase(private val modbusRepository: ModbusRepository) {
    suspend operator fun invoke() = modbusRepository.detenerProceso()
}

class ConnectModbusUseCase(private val modbusRepository: ModbusRepository) {
    suspend operator fun invoke(host: String, port: Int) = modbusRepository.connect(host, port)
}

class DisconnectModbusUseCase(private val modbusRepository: ModbusRepository) {
    suspend operator fun invoke() = modbusRepository.disconnect()
}
