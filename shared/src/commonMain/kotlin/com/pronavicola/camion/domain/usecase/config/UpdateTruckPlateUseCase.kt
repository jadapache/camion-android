package com.pronavicola.camion.domain.usecase.config

import com.pronavicola.camion.data.local.dao.ParametroDao
import com.pronavicola.camion.data.local.dao.VehiculoDao
import com.pronavicola.camion.data.local.entity.VehiculoEntity
import com.pronavicola.camion.domain.repository.AppConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateTruckPlateUseCase(
    private val vehiculoDao: VehiculoDao,
    private val parametroDao: ParametroDao,
    private val appConfigRepository: AppConfigRepository,
) {
    suspend operator fun invoke(rawPlate: String): Result<VehiculoEntity> = withContext(Dispatchers.Default) {
        val plate = rawPlate.trim()
        if (plate.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("La placa no puede estar vacía"))
        }

        try {
            var vehiculo = vehiculoDao.getByPlaca(plate)
            if (vehiculo == null) {
                // 1. Desmarcar vehículos predeterminados existentes
                vehiculoDao.clearPredeterminado()

                // 2. Registrar nuevo vehículo en Vehiculos con Predeterminado = 1
                val newId = vehiculoDao.insert(
                    VehiculoEntity(
                        placa = plate,
                        predeterminado = 1,
                    )
                ).toInt()

                // 3. Copiar el listado de parámetros del IdVehiculo = 1 (Camión de prueba) al nuevo IdVehiculo
                val baseParams = parametroDao.getByVehiculo(1)
                val newParams = baseParams.map { baseParam ->
                    baseParam.copy(idVehiculo = newId)
                }
                if (newParams.isNotEmpty()) {
                    parametroDao.insertAll(newParams)
                }

                vehiculo = vehiculoDao.getById(newId)
                    ?: VehiculoEntity(idVehiculo = newId, placa = plate, predeterminado = 1)
            } else {
                // Si la placa ya existe, asegurarse de que quede activa / predeterminada
                vehiculoDao.clearPredeterminado()
                vehiculoDao.setPredeterminado(vehiculo.idVehiculo)

                // Si por alguna razón no tuviera parámetros, copiar desde IdVehiculo = 1
                val existingParams = parametroDao.getByVehiculo(vehiculo.idVehiculo)
                if (existingParams.isEmpty() && vehiculo.idVehiculo != 1) {
                    val baseParams = parametroDao.getByVehiculo(1)
                    val newParams = baseParams.map { baseParam ->
                        baseParam.copy(idVehiculo = vehiculo.idVehiculo)
                    }
                    if (newParams.isNotEmpty()) {
                        parametroDao.insertAll(newParams)
                    }
                }

                vehiculo = vehiculo.copy(predeterminado = 1)
            }

            // 4. Sincronizar con AppConfig (deviceId y furgonPlate)
            appConfigRepository.saveDeviceId(plate)
            appConfigRepository.saveFurgonPlate(plate)

            Result.success(vehiculo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
