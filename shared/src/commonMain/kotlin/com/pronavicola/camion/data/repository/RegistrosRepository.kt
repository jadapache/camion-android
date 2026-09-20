package com.pronavicola.camion.data.repository

import com.pronavicola.camion.data.local.dao.VariableRegistroDao
import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.domain.repository.RegistrosRepository
import kotlinx.coroutines.flow.Flow

class RegistrosRepositoryImpl(
    private val variableRegistroDao: VariableRegistroDao,
) : RegistrosRepository {
    override fun observeRegistros(user: SessionUser): Flow<List<VariableRegistroEntity>> =
        variableRegistroDao.observeRegistros(user.idVehiculo)

    override suspend fun saveRegistro(registro: VariableRegistroEntity) {
        variableRegistroDao.insert(registro)
    }

    override suspend fun updateEstadoEnvio(idRegistro: Int, estado: String) {
        variableRegistroDao.updateEstadoEnvio(idRegistro, estado)
    }

    override suspend fun clearAllRegistros() {
        variableRegistroDao.deleteAll()
    }
}
