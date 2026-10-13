package com.pronavicola.camion.data.repository

import com.pronavicola.camion.data.local.dao.VariableRegistroDao
import com.pronavicola.camion.data.local.dao.VehiculoDao
import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.data.local.entity.VariablesRegistrosEntity
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.domain.repository.RegistrosRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrosRepositoryImpl(
    private val variableRegistroDao: VariableRegistroDao,
    private val vehiculoDao: VehiculoDao,
) : RegistrosRepository {
    override fun observeRegistros(user: SessionUser): Flow<List<VariableRegistroEntity>> =
        variableRegistroDao.observeAllRegistros()

    override suspend fun getRegistrosPaged(idVehiculo: Int?, limit: Int, offset: Int): List<VariableRegistroEntity> =
        variableRegistroDao.getRegistrosPaged(idVehiculo, limit, offset)

    override suspend fun getRegistrosCount(idVehiculo: Int?): Int =
        variableRegistroDao.getCount(idVehiculo)

    override suspend fun saveRegistro(registro: VariableRegistroEntity) {
        val vehiculoId = vehiculoDao.getPredeterminado()?.idVehiculo ?: registro.idVehiculo
        val rawEntries = listOf(
            VariablesRegistrosEntity(
                idVehiculo = vehiculoId,
                idVariable = 1,
                fechaHora = registro.fecha,
                valor = registro.temperatura1,
                estado = registro.estadoEnvio,
            ),
            VariablesRegistrosEntity(
                idVehiculo = vehiculoId,
                idVariable = 2,
                fechaHora = registro.fecha,
                valor = registro.temperatura2,
                estado = registro.estadoEnvio,
            ),
            VariablesRegistrosEntity(
                idVehiculo = vehiculoId,
                idVariable = 4,
                fechaHora = registro.fecha,
                valor = registro.humedad,
                estado = registro.estadoEnvio,
            ),
            VariablesRegistrosEntity(
                idVehiculo = vehiculoId,
                idVariable = 5,
                fechaHora = registro.fecha,
                valor = registro.tension,
                estado = registro.estadoEnvio,
            ),
        )
        variableRegistroDao.insertRawAll(rawEntries)
    }

    override suspend fun updateEstadoEnvioByFecha(fecha: String, estado: String) {
        variableRegistroDao.updateEstadoEnvioByFecha(fecha, estado)
    }

    override suspend fun markAllPendingAsEnviado() {
        variableRegistroDao.markAllPendingAsEnviado()
    }

    override suspend fun clearAllRegistros() {
        variableRegistroDao.deleteAll()
    }
}
