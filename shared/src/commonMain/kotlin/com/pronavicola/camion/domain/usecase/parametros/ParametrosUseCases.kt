package com.pronavicola.camion.domain.usecase.parametros

import com.pronavicola.camion.domain.model.ParametroUiModel
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.domain.repository.ParametrosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveParametrosUseCase(
    private val parametrosRepository: ParametrosRepository,
) {
    operator fun invoke(user: SessionUser): Flow<List<ParametroUiModel>> {
        return parametrosRepository.observeParametros(user).map { list ->
            parametrosRepository.enrichWithPermissions(list, user)
        }
    }
}

class UpdateParametroUseCase(
    private val parametrosRepository: ParametrosRepository,
) {
    suspend operator fun invoke(
        user: SessionUser,
        parametro: ParametroUiModel,
        nuevoValor: Double,
    ): Result<Double> {
        return parametrosRepository.updateParametro(user, parametro, nuevoValor)
    }
}
