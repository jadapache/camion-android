package com.pronavicola.camion.domain.repository

import com.pronavicola.camion.domain.model.ParametroUiModel
import com.pronavicola.camion.domain.model.SessionUser
import kotlinx.coroutines.flow.Flow

interface ParametrosRepository {
    fun observeParametros(user: SessionUser): Flow<List<ParametroUiModel>>
    suspend fun enrichWithPermissions(
        parametros: List<ParametroUiModel>,
        user: SessionUser,
    ): List<ParametroUiModel>
    suspend fun updateParametro(
        user: SessionUser,
        parametro: ParametroUiModel,
        nuevoValor: Double,
    ): Result<Double>
}
