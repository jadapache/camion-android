package com.pronavicola.camion.domain.repository

import com.pronavicola.camion.data.local.entity.UsuarioEntity
import com.pronavicola.camion.domain.model.SessionUser
import kotlinx.coroutines.flow.Flow

sealed class AuthResult {
    data class Success(val user: SessionUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

interface AuthRepository {
    val activeUsers: Flow<List<UsuarioEntity>>
    suspend fun loadDefaultUser(): SessionUser
    suspend fun switchUser(
        currentUser: SessionUser,
        targetIdUsuario: String,
        password: String,
    ): AuthResult
    suspend fun changePassword(
        currentUser: SessionUser,
        currentPassword: String,
        newPassword: String,
    ): AuthResult
    suspend fun updateDeviceId(userId: String, deviceId: String)
    fun canAccessScreen(idPerfil: Int, accessLevel: Int): Boolean
}
