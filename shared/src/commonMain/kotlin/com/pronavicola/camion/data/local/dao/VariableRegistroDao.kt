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

    @Query("SELECT * FROM V_VariablesRegistro WHERE (:idVehiculo IS NULL OR IdVehiculo = :idVehiculo) ORDER BY Fecha DESC LIMIT :limit OFFSET :offset")
    suspend fun getRegistrosPaged(idVehiculo: Int?, limit: Int, offset: Int): List<VariableRegistroEntity>

    @Query("SELECT COUNT(*) FROM V_VariablesRegistro WHERE (:idVehiculo IS NULL OR IdVehiculo = :idVehiculo)")
    suspend fun getCount(idVehiculo: Int? = null): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawAll(registros: List<VariablesRegistrosEntity>)

    @Query("UPDATE VariablesRegistros SET Estado = :estado WHERE FechaHora = :fecha")
    suspend fun updateEstadoEnvioByFecha(fecha: String, estado: String)

    @Query("UPDATE VariablesRegistros SET Estado = 'Enviado' WHERE UPPER(COALESCE(Estado, 'PENDIENTE')) != 'ENVIADO'")
    suspend fun markAllPendingAsEnviado()

    @Query("DELETE FROM VariablesRegistros WHERE rowid IN (SELECT rowid FROM VariablesRegistros WHERE FechaHora < :cutoffDateStr LIMIT :limit)")
    suspend fun deleteOlderThanBatch(cutoffDateStr: String, limit: Int = 1000): Int

    @Query("DELETE FROM VariablesRegistros")
    suspend fun deleteAll()
}
