package com.pronavicola.camion.domain.usecase.alarmas

import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.domain.repository.AlarmaRegistroRepository

class ClearAllAlarmasRegistrosUseCase(
    private val alarmaRegistroRepository: AlarmaRegistroRepository,
) {
    suspend operator fun invoke(user: SessionUser?): Boolean {
        // Only Administrador (1), Supervisor (2), Desarrollador (0)
        return if (user != null && user.idPerfil in listOf(0, 1, 2)) {
            alarmaRegistroRepository.clearAllAlarmasRegistros()
            true
        } else {
            false
        }
    }
}
