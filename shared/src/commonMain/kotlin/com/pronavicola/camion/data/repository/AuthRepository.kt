package com.pronavicola.camion.data.repository

import com.pronavicola.camion.data.local.dao.PerfilDao
import com.pronavicola.camion.data.local.dao.UsuarioDao
import com.pronavicola.camion.data.local.entity.PerfilEntity
import com.pronavicola.camion.data.local.entity.UsuarioEntity
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.domain.repository.AuthRepository
import com.pronavicola.camion.domain.repository.AuthResult
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl(
    private val usuarioDao: UsuarioDao,
    private val perfilDao: PerfilDao,
) : AuthRepository {
    override val activeUsers: Flow<List<UsuarioEntity>> = usuarioDao.observeActiveUsers()

    override suspend fun loadDefaultUser(): SessionUser {
        ensureDefaultUsersExist()
        val user = usuarioDao.getDefaultUser()
            ?: usuarioDao.getById("OPERARIO")
            ?: usuarioDao.getById("ADMIN")
            ?: usuarioDao.getById("DESARROLLADOR")
        return user?.toSession() ?: SessionUser(
            idUsuario = "OPERARIO",
            usuario = "Operario",
            idPerfil = 4,
            idVehiculo = 1,
        )
    }

    private suspend fun ensureDefaultUsersExist() {
        val defaultProfiles = listOf(
            PerfilEntity(0, "Desarrollador"),
            PerfilEntity(1, "Administrador"),
            PerfilEntity(2, "Supervisor"),
            PerfilEntity(3, "Técnico"),
            PerfilEntity(4, "Operario"),
        )
        for (perfil in defaultProfiles) {
            perfilDao.insert(perfil)
        }

        val defaultUsers = listOf(
            UsuarioEntity(
                idUsuario = "OPERARIO",
                usuario = "Operario",
                password = "",
                idPerfil = 4,
                email = "operario@camion.local",
                estado = "ESTAUSUA.ACT",
            ),
            UsuarioEntity(
                idUsuario = "TECNICO",
                usuario = "Técnico",
                password = "123",
                idPerfil = 3,
                email = "tecnico@camion.local",
                estado = "ESTAUSUA.ACT",
            ),
            UsuarioEntity(
                idUsuario = "SUPERVISOR",
                usuario = "Supervisor",
                password = "123",
                idPerfil = 2,
                email = "supervisor@camion.local",
                estado = "ESTAUSUA.ACT",
            ),
            UsuarioEntity(
                idUsuario = "ADMIN",
                usuario = "Administrador",
                password = "123",
                idPerfil = 1,
                email = "admin@camion.local",
                estado = "ESTAUSUA.ACT",
            ),
            UsuarioEntity(
                idUsuario = "DESARROLLADOR",
                usuario = "Desarrollador",
                password = "SuperUser123",
                idPerfil = 0,
                email = "dev@camion.local",
                estado = "ESTAUSUA.ACT",
            ),
        )

        for (u in defaultUsers) {
            val existing = usuarioDao.getById(u.idUsuario)
            if (existing == null) {
                usuarioDao.insert(u)
            } else if (existing.estado == "ESTADO.ACT") {
                usuarioDao.update(existing.copy(estado = "ESTAUSUA.ACT"))
            }
        }
    }

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
