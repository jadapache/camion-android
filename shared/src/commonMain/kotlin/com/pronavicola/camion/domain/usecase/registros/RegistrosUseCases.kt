package com.pronavicola.camion.domain.usecase.registros

import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
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
        if (success) {
            registrosRepository.updateEstadoEnvio(registro.idRegistro, "Enviado")
        } else {
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
        val isoTimestamp = formatIsoTimestamp(registro.fecha)
        return """
        {
          "device_id": "$deviceId",
          "timestamp": "$isoTimestamp",
          "temperatura_1": ${registro.temperatura1},
          "temperatura_2": ${registro.temperatura2},
          "humedad": ${registro.humedad},
          "tension": ${registro.tension},
          "id_registro_local": ${registro.idRegistro}
        }
        """.trimIndent()
    }

    private fun formatIsoTimestamp(fechaStr: String): String {
        return try {
            if (fechaStr.contains(" ") && !fechaStr.contains("T")) {
                val parts = fechaStr.split(" ")
                "${parts[0]}T${parts[1]}.000Z"
            } else if (fechaStr.contains("T")) {
                if (fechaStr.endsWith("Z")) fechaStr else "${fechaStr}Z"
            } else {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000'Z'", Locale.getDefault()).format(Date())
            }
        } catch (_: Exception) {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000'Z'", Locale.getDefault()).format(Date())
        }
    }
}

class ExportRegistrosCsvUseCase {
    operator fun invoke(data: List<VariableRegistroEntity>): String {
        val sb = StringBuilder()
        sb.append("IdRegistro,IdVehiculo,Fecha,Temperatura1,Temperatura2,Humedad,Tension,EstadoEnvio\n")
        data.forEach { r ->
            sb.append("${r.idRegistro},\"${r.idVehiculo}\",\"${r.fecha}\",${r.temperatura1},${r.temperatura2},${r.humedad},${r.tension},\"${r.estadoEnvio}\"\n")
        }
        return sb.toString()
    }
}
