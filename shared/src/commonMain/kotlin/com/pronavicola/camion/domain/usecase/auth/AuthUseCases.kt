package com.pronavicola.camion.domain.usecase.auth

import com.pronavicola.camion.data.local.entity.UsuarioEntity
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.domain.repository.AuthRepository
import com.pronavicola.camion.domain.repository.AuthResult
import kotlinx.coroutines.flow.Flow

class GetActiveUsersUseCase(private val authRepository: AuthRepository) {
    operator fun invoke(): Flow<List<UsuarioEntity>> = authRepository.activeUsers
}

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(currentUser: SessionUser, targetIdUsuario: String, password: String): AuthResult {
        return authRepository.switchUser(currentUser, targetIdUsuario, password)
    }

    suspend fun loadDefaultUser(): SessionUser = authRepository.loadDefaultUser()
}

class ChangePasswordUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(currentUser: SessionUser, currentPassword: String, newPassword: String): AuthResult {
        return authRepository.changePassword(currentUser, currentPassword, newPassword)
    }
}

class CheckAccessPermissionUseCase(private val authRepository: AuthRepository) {
    operator fun invoke(user: SessionUser, requiredAccessLevel: Int): Boolean {
        // Desarrollador (0) has all permissions
        if (user.idPerfil == 0) return true
        return authRepository.canAccessScreen(user.idPerfil, requiredAccessLevel)
    }

    fun canClearRegistros(user: SessionUser): Boolean {
        // Admin (1), Supervisor (2), Desarrollador (0)
        return user.idPerfil == 0 || user.idPerfil == 1 || user.idPerfil == 2
    }

    fun isDeveloper(user: SessionUser): Boolean = user.idPerfil == 0
}
