package com.pronavicola.camion.domain.usecase.config

import com.pronavicola.camion.data.local.dao.ParametroDao
import com.pronavicola.camion.data.local.dao.VehiculoDao
import com.pronavicola.camion.data.local.entity.VehiculoEntity
import com.pronavicola.camion.data.mqtt.TelemetryMqttCoordinator
import com.pronavicola.camion.domain.repository.AppConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateTruckPlateUseCase(
    private val vehiculoDao: VehiculoDao,
    private val parametroDao: ParametroDao,
    private val appConfigRepository: AppConfigRepository,
    private val telemetryMqttCoordinator: TelemetryMqttCoordinator? = null,
) {
    suspend operator fun invoke(rawPlate: String): Result<VehiculoEntity> = withContext(Dispatchers.Default) {
        val plate = rawPlate.trim()
        if (plate.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("La placa no puede estar vacía"))
        }

        try {
            val existingDeviceId = appConfigRepository.getDeviceId()
            if (!existingDeviceId.isNullOrBlank() && plate.equals(existingDeviceId, ignoreCase = true)) {
                // Prevent assigning deviceId as the truck license plate
                return@withContext Result.failure(IllegalArgumentException("El Device ID ($plate) no puede ser asignado como placa"))
            }

            // Update active vehicle in place to NEVER orphan parameters or historical records
            val currentVehiculo = vehiculoDao.getPredeterminado() ?: vehiculoDao.getById(1)
            val updatedVehiculo = if (currentVehiculo != null) {
                val updated = currentVehiculo.copy(placa = plate, predeterminado = 1)
                vehiculoDao.update(updated)
                updated
            } else {
                val newVehiculo = VehiculoEntity(idVehiculo = 1, placa = plate, predeterminado = 1)
                vehiculoDao.insert(newVehiculo)
                newVehiculo
            }

            // Ensure vehicle 1 is active and clear any other predeterminados
            vehiculoDao.clearPredeterminado()
            vehiculoDao.setPredeterminado(updatedVehiculo.idVehiculo)

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
