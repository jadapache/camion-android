package com.pronavicola.camion

import com.pronavicola.camion.data.local.dao.VehiculoDao
import com.pronavicola.camion.data.local.entity.VehiculoEntity
import com.pronavicola.camion.domain.repository.AppConfigRepository
import com.pronavicola.camion.domain.usecase.config.UpdateTruckPlateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UpdateTruckPlateUseCaseTest {

    private class FakeVehiculoDao : VehiculoDao {
        val vehiculos = mutableListOf<VehiculoEntity>()
        private var nextId = 1

        init {
            vehiculos.add(VehiculoEntity(idVehiculo = 1, placa = "ABC123", predeterminado = 1))
            nextId = 2
        }

        override fun observeAll(): Flow<List<VehiculoEntity>> = emptyFlow()
        override fun observePredeterminado(): Flow<VehiculoEntity?> = emptyFlow()
        override suspend fun getPredeterminado(): VehiculoEntity? = vehiculos.firstOrNull { it.predeterminado == 1 }
        override suspend fun getById(idVehiculo: Int): VehiculoEntity? = vehiculos.firstOrNull { it.idVehiculo == idVehiculo }
        override suspend fun getByPlaca(placa: String): VehiculoEntity? =
            vehiculos.firstOrNull { it.placa.trim().equals(placa.trim(), ignoreCase = true) }

        override suspend fun insert(vehiculo: VehiculoEntity): Long {
            val id = if (vehiculo.idVehiculo > 0) vehiculo.idVehiculo else nextId++
            vehiculos.add(vehiculo.copy(idVehiculo = id))
            return id.toLong()
        }

        override suspend fun update(vehiculo: VehiculoEntity) {
            val idx = vehiculos.indexOfFirst { it.idVehiculo == vehiculo.idVehiculo }
            if (idx >= 0) vehiculos[idx] = vehiculo
        }

        override suspend fun deleteById(idVehiculo: Int) {
            vehiculos.removeAll { it.idVehiculo == idVehiculo }
        }

        override suspend fun clearPredeterminado() {
            for (i in vehiculos.indices) {
                if (vehiculos[i].predeterminado == 1) {
                    vehiculos[i] = vehiculos[i].copy(predeterminado = 0)
                }
            }
        }

        override suspend fun setPredeterminado(idVehiculo: Int) {
            for (i in vehiculos.indices) {
                if (vehiculos[i].idVehiculo == idVehiculo) {
                    vehiculos[i] = vehiculos[i].copy(predeterminado = 1)
                }
            }
        }
    }

    private class FakeAppConfigRepository : AppConfigRepository {
        var deviceId: String? = null
        var furgonPlate: String? = null

        override suspend fun getBrokerUrl(): String? = null
        override suspend fun saveBrokerUrl(url: String) {}
        override suspend fun getBrokerUsername(): String? = null
        override suspend fun saveBrokerUsername(username: String) {}
        override suspend fun getBrokerPassword(): String? = null
        override suspend fun saveBrokerPassword(password: String) {}
        override suspend fun getTopicRoot(): String? = null
        override suspend fun saveTopicRoot(root: String) {}
        override suspend fun getModbusHost(): String? = null
        override suspend fun saveModbusHost(host: String) {}
        override suspend fun getModbusPort(): String? = null
        override suspend fun saveModbusPort(port: String) {}
        override suspend fun getPublishInterval(): String? = null
        override suspend fun savePublishInterval(interval: String) {}
        override suspend fun getDeviceId(): String? = deviceId
        override suspend fun saveDeviceId(id: String) { deviceId = id }
        override suspend fun getFurgonPlate(): String? = furgonPlate
        override suspend fun saveFurgonPlate(plate: String) { furgonPlate = plate }
        override suspend fun getServerBaseUrl(): String? = null
        override suspend fun saveServerBaseUrl(url: String) {}
        override suspend fun getDeviceToken(): String? = null
        override suspend fun saveDeviceToken(token: String) {}
        override suspend fun getPairingCode(): String? = null
        override suspend fun savePairingCode(code: String) {}
        override suspend fun getLicenseKey(): String? = null
        override suspend fun saveLicenseKey(key: String) {}
        override suspend fun getLicenseStatus(): String? = null
        override suspend fun saveLicenseStatus(status: String) {}
        override suspend fun getModbusSimulationEnabled(): Boolean = false
        override suspend fun saveModbusSimulationEnabled(enabled: Boolean) {}
        override suspend fun getFurgonId(): String? = null
        override suspend fun saveFurgonId(id: String) {}
        override suspend fun getTokenExpiry(): Long? = null
        override suspend fun saveTokenExpiry(expiry: Long) {}
        override suspend fun getString(key: String): String? = null
        override suspend fun putString(key: String, value: String) {}
        override suspend fun getLong(key: String): Long? = null
        override suspend fun putLong(key: String, value: Long) {}
        override suspend fun remove(key: String) {}
    }

    @Test
    fun testUpdateTruckPlate_WhenPlateDoesNotExist_CreatesNewVehicleAndSetsPredeterminado() = runTest {
        val fakeVehiculoDao = FakeVehiculoDao()
        val fakeAppConfigRepo = FakeAppConfigRepository()

        val useCase = UpdateTruckPlateUseCase(
            vehiculoDao = fakeVehiculoDao,
            appConfigRepository = fakeAppConfigRepo,
        )

        val newPlate = "FURG-002"
        val result = useCase(newPlate)

        assertTrue(result.isSuccess)
        val registeredVehiculo = result.getOrThrow()
        assertEquals("FURG-002", registeredVehiculo.placa)
        assertEquals(1, registeredVehiculo.predeterminado)
        assertEquals(2, registeredVehiculo.idVehiculo)

        // Previous vehicle 1 is now predeterminado = 0
        val v1 = fakeVehiculoDao.getById(1)
        assertNotNull(v1)
        assertEquals(0, v1.predeterminado)
        assertEquals("ABC123", v1.placa)

        // Newly created vehicle 2 is predeterminado = 1
        val v2 = fakeVehiculoDao.getById(2)
        assertNotNull(v2)
        assertEquals(1, v2.predeterminado)
        assertEquals("FURG-002", v2.placa)

        // AppConfig was synced with furgonPlate
        assertEquals("FURG-002", fakeAppConfigRepo.furgonPlate)
    }

    @Test
    fun testUpdateTruckPlate_WhenPlateMatchesDeviceId_RejectsAssignment() = runTest {
        val fakeVehiculoDao = FakeVehiculoDao()
        val fakeAppConfigRepo = FakeAppConfigRepository().apply {
            deviceId = "BW7JTW7Z2QP"
        }

        val useCase = UpdateTruckPlateUseCase(
            vehiculoDao = fakeVehiculoDao,
            appConfigRepository = fakeAppConfigRepo,
        )

        val result = useCase("BW7JTW7Z2QP")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Device ID") == true)
    }

    @Test
    fun testUpdateTruckPlate_WhenPlateAlreadyExists_ActivatesItWithoutDuplicating() = runTest {
        val fakeVehiculoDao = FakeVehiculoDao()
        val fakeAppConfigRepo = FakeAppConfigRepository()

        val useCase = UpdateTruckPlateUseCase(
            vehiculoDao = fakeVehiculoDao,
            appConfigRepository = fakeAppConfigRepo,
        )

        // First add FURG-002
        useCase("FURG-002")
        assertEquals(2, fakeVehiculoDao.vehiculos.size)
        assertEquals(1, fakeVehiculoDao.getById(2)?.predeterminado)

        // Now select ABC123 which already exists as ID 1
        val result = useCase("abc123")

        assertTrue(result.isSuccess)
        val vehiculo = result.getOrThrow()
        assertEquals(1, vehiculo.idVehiculo)
        assertEquals(1, vehiculo.predeterminado)

        // Still only 2 vehicles in table (no duplicates)
        assertEquals(2, fakeVehiculoDao.vehiculos.size)
        assertEquals(0, fakeVehiculoDao.getById(2)?.predeterminado)
    }
}
