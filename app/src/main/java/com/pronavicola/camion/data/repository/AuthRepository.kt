package com.pronavicola.camion.data.repository

import com.pronavicola.camion.data.local.dao.UsuarioDao
import com.pronavicola.camion.data.local.entity.UsuarioEntity
import com.pronavicola.camion.domain.model.SessionUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed class AuthResult {
    data class Success(val user: SessionUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    private val usuarioDao: UsuarioDao,
) {
    val activeUsers: Flow<List<UsuarioEntity>> = usuarioDao.observeActiveUsers()

    suspend fun loadDefaultUser(): SessionUser {
        val user = usuarioDao.getDefaultUser()
            ?: usuarioDao.getById("OPERARIO")
            ?: error("Usuario OPERARIO no encontrado en Furgones.db")
        return user.toSession()
    }

    /**
     * Mirrors legacy Config.xaml.vb login rules:
     * - Lower IdPerfil = higher privilege (Admin=1, Operario=4)
     * - Switching to equal/lower privilege requires password
     * - Switching to higher privilege (target IdPerfil > current) is allowed without password
     */
    suspend fun switchUser(
        currentUser: SessionUser,
        targetIdUsuario: String,
        password: String,
    ): AuthResult {
        if (currentUser.idUsuario == targetIdUsuario) {
            return AuthResult.Error("Ya esta logueado con: ${currentUser.usuario}")
        }

        val target = usuarioDao.getById(targetIdUsuario)
            ?: return AuthResult.Error("Usuario no encontrado")

        if (currentUser.idPerfil < target.idPerfil) {
            return AuthResult.Success(target.toSession())
        }

        if (password.isBlank() || password != target.password) {
            return AuthResult.Error("Password no coincide")
        }

        return AuthResult.Success(target.toSession())
    }

    suspend fun changePassword(
        currentUser: SessionUser,
        currentPassword: String,
        newPassword: String,
    ): AuthResult {
        val user = usuarioDao.getById(currentUser.idUsuario)
            ?: return AuthResult.Error("Usuario no encontrado")

        if (currentPassword.isBlank() || newPassword.isBlank() || currentPassword != user.password) {
            return AuthResult.Error("Password no coincide")
        }

        usuarioDao.update(user.copy(password = newPassword))
        return AuthResult.Success(user.copy(password = newPassword).toSession())
    }

    suspend fun updateDeviceId(userId: String, deviceId: String) {
        val user = usuarioDao.getById(userId) ?: return
        usuarioDao.update(user.copy(idVehiculo = deviceId))
    }

    fun canAccessScreen(idPerfil: Int, accessLevel: Int): Boolean = idPerfil <= accessLevel

    private fun UsuarioEntity.toSession(): SessionUser = SessionUser(
        idUsuario = idUsuario,
        usuario = usuario,
        idPerfil = idPerfil,
        idVehiculo = idVehiculo ?: "CAM01",
    )
}
