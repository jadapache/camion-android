package com.pronavicola.camion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.data.local.entity.VariablesRegistrosEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VariableRegistroDao {
    @Query("SELECT * FROM V_VariablesRegistro ORDER BY Fecha DESC")
    fun observeAllRegistros(): Flow<List<VariableRegistroEntity>>

    @Query("SELECT * FROM V_VariablesRegistro WHERE IdVehiculo = :idVehiculo ORDER BY Fecha DESC")
    fun observeRegistros(idVehiculo: Int): Flow<List<VariableRegistroEntity>>

    @Query("SELECT * FROM V_VariablesRegistro WHERE UPPER(EstadoEnvio) != 'ENVIADO' ORDER BY Fecha ASC")
    suspend fun getAllPendingRegistros(): List<VariableRegistroEntity>

    @Query("SELECT * FROM V_VariablesRegistro WHERE IdVehiculo = :idVehiculo AND UPPER(EstadoEnvio) != 'ENVIADO' ORDER BY Fecha ASC")
    suspend fun getPendingRegistros(idVehiculo: Int): List<VariableRegistroEntity>

    @Query("SELECT * FROM V_VariablesRegistro WHERE (:idVehiculo IS NULL OR IdVehiculo = :idVehiculo) ORDER BY Fecha DESC LIMIT :limit OFFSET :offset")
    suspend fun getRegistrosPaged(idVehiculo: Int?, limit: Int, offset: Int): List<VariableRegistroEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaw(registro: VariablesRegistrosEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawAll(registros: List<VariablesRegistrosEntity>)

    @Query("UPDATE VariablesRegistros SET Estado = :estado WHERE FechaHora = (SELECT Fecha FROM V_VariablesRegistro WHERE IdRegistro = :idRegistro LIMIT 1)")
    suspend fun updateEstadoEnvio(idRegistro: Int, estado: String)

    @Query("UPDATE VariablesRegistros SET Estado = :estado WHERE FechaHora = :fecha")
    suspend fun updateEstadoEnvioByFecha(fecha: String, estado: String)

    @Query("UPDATE VariablesRegistros SET Estado = :estado WHERE IdVehiculo = :idVehiculo AND FechaHora = :fechaHora")
    suspend fun updateEstado(idVehiculo: Int, fechaHora: String, estado: String)

    @Query("DELETE FROM VariablesRegistros WHERE rowid IN (SELECT rowid FROM VariablesRegistros WHERE FechaHora < :cutoffDateStr LIMIT :limit)")
    suspend fun deleteOlderThanBatch(cutoffDateStr: String, limit: Int = 1000): Int

    @Query("DELETE FROM VariablesRegistros")
    suspend fun deleteAll()
}
