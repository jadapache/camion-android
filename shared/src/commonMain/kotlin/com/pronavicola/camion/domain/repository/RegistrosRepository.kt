package com.pronavicola.camion.domain.repository

import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.domain.model.SessionUser
import kotlinx.coroutines.flow.Flow

interface RegistrosRepository {
    fun observeRegistros(user: SessionUser): Flow<List<VariableRegistroEntity>>
    suspend fun saveRegistro(registro: VariableRegistroEntity)
    suspend fun updateEstadoEnvio(idRegistro: Int, estado: String)
    suspend fun clearAllRegistros()
}
