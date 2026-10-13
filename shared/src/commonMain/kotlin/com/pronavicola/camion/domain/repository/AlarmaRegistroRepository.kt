package com.pronavicola.camion.domain.repository

import com.pronavicola.camion.data.local.entity.AlarmaEntity
import com.pronavicola.camion.data.local.entity.AlarmaRegistroEntity
import kotlinx.coroutines.flow.Flow

interface AlarmaRegistroRepository {
    fun observeAlarmasRegistros(idVehiculo: Int): Flow<List<AlarmaRegistroEntity>>
    fun observeAllAlarmasRegistros(): Flow<List<AlarmaRegistroEntity>>
    suspend fun getAlarmas(): List<AlarmaEntity>
    suspend fun clearAllAlarmasRegistros()
}
