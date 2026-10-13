package com.pronavicola.camion.domain.usecase.registros

import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.data.mqtt.TelemetryMqttPayload
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.domain.repository.MqttRepository
import com.pronavicola.camion.domain.repository.RegistrosRepository
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ObserveRegistrosUseCase(
    private val registrosRepository: RegistrosRepository,
) {
    operator fun invoke(user: SessionUser): Flow<List<VariableRegistroEntity>> =
        registrosRepository.observeRegistros(user)
}

class GetRegistrosPagedUseCase(
    private val registrosRepository: RegistrosRepository,
) {
    suspend operator fun invoke(idVehiculo: Int?, limit: Int, offset: Int): List<VariableRegistroEntity> =
        registrosRepository.getRegistrosPaged(idVehiculo, limit, offset)

    suspend fun getCount(idVehiculo: Int? = null): Int =
        registrosRepository.getRegistrosCount(idVehiculo)
}

class SaveTelemetryRegistroUseCase(
    private val registrosRepository: RegistrosRepository,
) {
    suspend operator fun invoke(registro: VariableRegistroEntity) {
        registrosRepository.saveRegistro(registro)
    }
}

class ResendRegistroUseCase(
    private val registrosRepository: RegistrosRepository,
    private val mqttRepository: MqttRepository,
) {
    suspend operator fun invoke(registro: VariableRegistroEntity, deviceId: String): Boolean {
        val topic = "furgones/$deviceId/telemetry"
        val payload = BuildTelemetryPayloadUseCase()(registro, deviceId)
        val success = mqttRepository.publish(topic, payload, qos = 1)
        if (!success) {
            mqttRepository.enqueueOutbox(topic, payload, qos = 1)
        }
        return success
    }
}

class ClearAllRegistrosUseCase(
    private val registrosRepository: RegistrosRepository,
) {
    suspend operator fun invoke(user: SessionUser): Boolean {
        // Desarrollador (0), Administrador (1), Supervisor (2)
        if (user.idPerfil in listOf(0, 1, 2)) {
            registrosRepository.clearAllRegistros()
            return true
        }
        return false
    }
}

class BuildTelemetryPayloadUseCase {
    operator fun invoke(registro: VariableRegistroEntity, deviceId: String): String {
        return TelemetryMqttPayload(
            deviceId = deviceId,
            temperatureC = registro.temperatura1,
            temperature2C = registro.temperatura2,
            humidityPct = registro.humedad,
            voltageV = registro.tension,
        ).toJson()
    }
}

class ExportRegistrosCsvUseCase {
    operator fun invoke(data: List<VariableRegistroEntity>): String {
        val sb = StringBuilder()
        sb.append("IdRegistro,IdVehiculo,Fecha,Temperatura1,Temperatura2,Humedad,Tension\n")
        data.forEach { r ->
            sb.append("${r.idRegistro},\"${r.idVehiculo}\",\"${r.fecha}\",${r.temperatura1},${r.temperatura2},${r.humedad},${r.tension}\n")
        }
        return sb.toString()
    }
}
