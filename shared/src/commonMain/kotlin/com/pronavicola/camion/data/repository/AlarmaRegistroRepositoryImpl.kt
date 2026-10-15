package com.pronavicola.camion.data.repository

import com.pronavicola.camion.data.local.dao.AlarmaDao
import com.pronavicola.camion.data.local.dao.AlarmaRegistroDao
import com.pronavicola.camion.data.local.entity.AlarmaEntity
import com.pronavicola.camion.data.local.entity.AlarmaRegistroEntity
import com.pronavicola.camion.domain.repository.AlarmaRegistroRepository
import kotlinx.coroutines.flow.Flow

class AlarmaRegistroRepositoryImpl(
    private val alarmaRegistroDao: AlarmaRegistroDao,
    private val alarmaDao: AlarmaDao
) : AlarmaRegistroRepository {
    override fun observeAlarmasRegistros(idVehiculo: Int): Flow<List<AlarmaRegistroEntity>> {
        return alarmaRegistroDao.observeByVehiculo(idVehiculo)
    }

    override suspend fun getAlarmas(): List<AlarmaEntity> {
        return alarmaDao.getAll()
    }
}
