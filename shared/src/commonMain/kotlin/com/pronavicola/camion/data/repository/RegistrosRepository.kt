package com.pronavicola.camion.data.repository

import com.pronavicola.camion.data.local.dao.VariableRegistroDao
import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.domain.model.SessionUser
import kotlinx.coroutines.flow.Flow

class RegistrosRepository(
    private val variableRegistroDao: VariableRegistroDao,
) {
    fun observeRegistros(user: SessionUser): Flow<List<VariableRegistroEntity>> =
        variableRegistroDao.observeRegistros(user.idVehiculo)

    suspend fun saveRegistro(registro: VariableRegistroEntity) {
        variableRegistroDao.insert(registro)
    }

    suspend fun updateEstadoEnvio(idRegistro: Int, estado: String) {
        variableRegistroDao.updateEstadoEnvio(idRegistro, estado)
    }
}
