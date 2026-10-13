package com.pronavicola.camion.domain.repository

import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.domain.model.SessionUser
import kotlinx.coroutines.flow.Flow

interface RegistrosRepository {
    fun observeRegistros(user: SessionUser): Flow<List<VariableRegistroEntity>>
    suspend fun getRegistrosPaged(idVehiculo: Int?, limit: Int, offset: Int): List<VariableRegistroEntity>
    suspend fun getRegistrosCount(idVehiculo: Int? = null): Int
    suspend fun saveRegistro(registro: VariableRegistroEntity)
    suspend fun updateEstadoEnvioByFecha(fecha: String, estado: String)
    suspend fun markAllPendingAsEnviado()
    suspend fun clearAllRegistros()
}
