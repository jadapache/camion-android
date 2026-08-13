package com.pronavicola.camion.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.pronavicola.camion.data.local.entity.ParametroEntity
import com.pronavicola.camion.data.local.entity.PerfilEntity
import com.pronavicola.camion.data.local.entity.UsuarioEntity
import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import kotlinx.coroutines.flow.Flow

data class ParametroConUnidad(
    val idVehiculo: String,
    val idParametro: Int,
    val parametro: String,
    val idTipo: String,
    val valor: Double,
    val idUnidad: String,
    val tag: String,
    val minimo: Double,
    val maximo: Double,
    val unidad: String,
)

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM Usuarios WHERE Estado = 'ESTADO.ACT' ORDER BY IdPerfil")
    fun observeActiveUsers(): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM Usuarios WHERE IdUsuario = :idUsuario LIMIT 1")
    suspend fun getById(idUsuario: String): UsuarioEntity?

    @Query("SELECT * FROM Usuarios WHERE IdUsuario = 'OPERARIO' LIMIT 1")
    suspend fun getDefaultUser(): UsuarioEntity?

    @Update
    suspend fun update(usuario: UsuarioEntity)
}

@Dao
interface PerfilDao {
    @Query("SELECT * FROM Perfiles WHERE IdPerfil = :idPerfil LIMIT 1")
    suspend fun getById(idPerfil: Int): PerfilEntity?
}

@Dao
interface ParametroDao {
    @Query(
        """
        SELECT p.IdVehiculo AS idVehiculo, p.IdParametro AS idParametro,
               p.Parametro AS parametro, p.IdTipo AS idTipo, p.Valor AS valor,
               p.IdUnidad AS idUnidad, p.Tag AS tag, p.Minimo AS minimo,
               p.Maximo AS maximo, d.Definicion AS unidad
        FROM Parametros p
        INNER JOIN Definiciones d ON d.IdDefinicion = p.IdUnidad
        WHERE p.IdVehiculo = :idVehiculo
        ORDER BY p.IdParametro
        """,
    )
    fun observeParametros(idVehiculo: String): Flow<List<ParametroConUnidad>>

    @Query(
        """
        SELECT COUNT(*) > 0 FROM PerfilesParametros
        WHERE IdPerfil = :idPerfil AND IdVehiculo = :idVehiculo AND IdParametro = :idParametro
        """,
    )
    suspend fun canEdit(idPerfil: Int, idVehiculo: String, idParametro: Int): Boolean

    @Query(
        """
        UPDATE Parametros SET Valor = :valor
        WHERE IdVehiculo = :idVehiculo AND IdParametro = :idParametro
        """,
    )
    suspend fun updateValor(idVehiculo: String, idParametro: Int, valor: Double)

    @Query(
        "SELECT * FROM Parametros WHERE IdVehiculo = :idVehiculo AND IdParametro = :idParametro LIMIT 1",
    )
    suspend fun getById(idVehiculo: String, idParametro: Int): ParametroEntity?
}

@Dao
interface VariableRegistroDao {
    @Query("SELECT * FROM VariableRegistros WHERE IdVehiculo = :idVehiculo ORDER BY Fecha DESC")
    fun observeRegistros(idVehiculo: String): Flow<List<VariableRegistroEntity>>

    @androidx.room.Insert
    suspend fun insert(registro: VariableRegistroEntity)
}

