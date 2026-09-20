package com.pronavicola.camion.data.repository

import com.pronavicola.camion.data.local.dao.UsuarioDao
import com.pronavicola.camion.data.local.entity.UsuarioEntity
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.domain.repository.AuthRepository
import com.pronavicola.camion.domain.repository.AuthResult
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl(
    private val usuarioDao: UsuarioDao,
) : AuthRepository {
    override val activeUsers: Flow<List<UsuarioEntity>> = usuarioDao.observeActiveUsers()

    override suspend fun loadDefaultUser(): SessionUser {
        ensureDesarrolladorExists()
        val user = usuarioDao.getDefaultUser()
            ?: usuarioDao.getById("OPERARIO")
            ?: error("Usuario OPERARIO no encontrado en Furgones.db")
        return user.toSession()
    }

    private suspend fun ensureDesarrolladorExists() {
        val dev = usuarioDao.getById("DESARROLLADOR")
        if (dev == null) {
            usuarioDao.insert(
                UsuarioEntity(
                    idUsuario = "DESARROLLADOR",
                    usuario = "Desarrollador",
                    password = "SuperUser123",
                    idPerfil = 0,
                    email = "dev@camion.local",
                    estado = "ESTAUSUA.ACT",
                ),
            )
        } else if (dev.password == "dev") {
            usuarioDao.update(dev.copy(password = "SuperUser123"))
        }
    }

    /**
     * Mirrors legacy Config.xaml.vb login rules:
     * - Lower IdPerfil = higher privilege (Admin=1, Operario=4, Desarrollador=0)
     * - Switching to equal/lower privilege requires password
     * - Switching to higher privilege (target IdPerfil > current) is allowed without password
     */
    override suspend fun switchUser(
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

        if (password.isBlank() || (password != target.password)) {
            return AuthResult.Error("Password no coincide")
        }

        return AuthResult.Success(target.toSession())
    }

    override suspend fun changePassword(
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

    override fun canAccessScreen(idPerfil: Int, accessLevel: Int): Boolean = idPerfil <= accessLevel

    private fun UsuarioEntity.toSession(): SessionUser = SessionUser(
        idUsuario = idUsuario,
        usuario = usuario,
        idPerfil = idPerfil,
        idVehiculo = 1,
    )
}
