package com.pronavicola.camion.domain.usecase.config

import com.pronavicola.camion.data.local.dao.VehiculoDao
import com.pronavicola.camion.data.local.entity.VehiculoEntity
import com.pronavicola.camion.data.mqtt.TelemetryMqttCoordinator
import com.pronavicola.camion.domain.repository.AppConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateTruckPlateUseCase(
    private val vehiculoDao: VehiculoDao,
    private val appConfigRepository: AppConfigRepository,
    private val telemetryMqttCoordinator: TelemetryMqttCoordinator? = null,
) {
    suspend operator fun invoke(rawPlate: String): Result<VehiculoEntity> = withContext(Dispatchers.Default) {
        val plate = rawPlate.trim().uppercase()
        if (plate.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("La placa no puede estar vacía"))
        }

        try {
            val existingDeviceId = appConfigRepository.getDeviceId()
            if (!existingDeviceId.isNullOrBlank() && plate.equals(existingDeviceId, ignoreCase = true)) {
                // Prevent assigning deviceId as the truck license plate
                return@withContext Result.failure(IllegalArgumentException("El Device ID ($plate) no puede ser asignado como placa"))
            }

            // Crear un registro por cada placa en la tabla Vehiculos y marcarla como 'Predeterminada' con el valor 1
            val existingVehiculo = vehiculoDao.getByPlaca(plate)
            val updatedVehiculo = if (existingVehiculo != null) {
                vehiculoDao.clearPredeterminado()
                vehiculoDao.setPredeterminado(existingVehiculo.idVehiculo)
                existingVehiculo.copy(predeterminado = 1)
            } else {
                vehiculoDao.clearPredeterminado()
                val newVehiculo = VehiculoEntity(idVehiculo = 0, placa = plate, predeterminado = 1)
                val newId = vehiculoDao.insert(newVehiculo)
                VehiculoEntity(idVehiculo = newId.toInt(), placa = plate, predeterminado = 1)
            }

            // Sincronizar con AppConfig (furgonPlate)
            appConfigRepository.saveFurgonPlate(plate)

            val activeDeviceId = if (!existingDeviceId.isNullOrBlank()) {
                existingDeviceId
            } else {
                "ABC123"
            }
            telemetryMqttCoordinator?.updateDeviceId(activeDeviceId)

            val topicRoot = appConfigRepository.getTopicRoot() ?: "furgones"
            telemetryMqttCoordinator?.updateTopicRoot(topicRoot)

            Result.success(updatedVehiculo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
