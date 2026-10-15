package com.pronavicola.camion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pronavicola.camion.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM Usuarios WHERE UPPER(Estado) IN ('ESTAUSUA.ACT', 'ESTADO.ACT', 'ACTIVO') OR UPPER(Estado) LIKE '%.ACT' ORDER BY IdPerfil")
    fun observeActiveUsers(): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM Usuarios WHERE UPPER(IdUsuario) = UPPER(:idUsuario) LIMIT 1")
    suspend fun getById(idUsuario: String): UsuarioEntity?

    @Query("SELECT * FROM Usuarios WHERE UPPER(IdUsuario) = 'OPERARIO' LIMIT 1")
    suspend fun getDefaultUser(): UsuarioEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(usuario: UsuarioEntity)

    @Update
    suspend fun update(usuario: UsuarioEntity)
}
