package com.pronavicola.camion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pronavicola.camion.data.local.entity.ParametroEntity
import kotlinx.coroutines.flow.Flow

data class ParametroConUnidad(
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
interface ParametroDao {
    @Query(
        """
        SELECT p.IdParametro AS idParametro,
               p.Parametro AS parametro, p.IdTipo AS idTipo, p.Valor AS valor,
               p.IdUnidad AS idUnidad, p.Tag AS tag, p.Minimo AS minimo,
               p.Maximo AS maximo, COALESCE(d.Definicion, p.IdUnidad) AS unidad
        FROM Parametros p
        LEFT JOIN Definiciones d ON d.IdDefinicion = p.IdUnidad
        ORDER BY p.IdParametro
        """,
    )
    fun observeParametros(): Flow<List<ParametroConUnidad>>

    @Query(
        """
        SELECT (:idPerfil IN (0, 1) OR (
            SELECT COUNT(*) FROM PerfilesParametros
            WHERE IdPerfil = :idPerfil AND IdParametro = :idParametro
        ) > 0)
        """,
    )
    suspend fun canEdit(idPerfil: Int, idParametro: Int): Boolean

    @Query(
        """
        UPDATE Parametros SET Valor = :valor
        WHERE IdParametro = :idParametro
        """,
    )
    suspend fun updateValor(idParametro: Int, valor: Double)

    @Query(
        "SELECT * FROM Parametros WHERE IdParametro = :idParametro LIMIT 1",
    )
    suspend fun getById(idParametro: Int): ParametroEntity?

    @Query("SELECT * FROM Parametros ORDER BY IdParametro ASC")
    suspend fun getAll(): List<ParametroEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(parametros: List<ParametroEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(parametro: ParametroEntity)
}
