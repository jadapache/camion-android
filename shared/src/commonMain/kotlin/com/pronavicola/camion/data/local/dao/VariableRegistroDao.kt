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

    @Query("DELETE FROM VariablesRegistros WHERE rowid IN (SELECT rowid FROM VariablesRegistros WHERE FechaHora < :cutoffDateStr LIMIT :limit)")
    suspend fun deleteOlderThanBatch(cutoffDateStr: String, limit: Int = 1000): Int

    @Query("DELETE FROM VariablesRegistros WHERE rowid IN (SELECT rowid FROM VariablesRegistros WHERE FechaHora < :cutoffTime LIMIT :limit)")
    suspend fun deleteOlderThanBatch(cutoffTime: String, limit: Int): Int

    @Query("SELECT COUNT(*) FROM V_VariablesRegistro WHERE UPPER(EstadoEnvio) = UPPER(:status)")
    suspend fun countByStatus(status: String): Int

    @Query("""
        SELECT v.* FROM V_VariablesRegistro v
        LEFT JOIN Vehiculos veh ON v.IdVehiculo = veh.IdVehiculo
        WHERE (:startDate IS NULL OR v.Fecha >= :startDate)
          AND (:endDate IS NULL OR v.Fecha <= :endDate)
          AND (:plate IS NULL OR veh.Placa LIKE '%' || :plate || '%')
          AND (:status IS NULL OR UPPER(v.EstadoEnvio) = UPPER(:status))
        ORDER BY v.Fecha DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getPagedRecords(
        limit: Int,
        offset: Int,
        startDate: String? = null,
        endDate: String? = null,
        plate: String? = null,
        status: String? = null,
    ): List<VariableRegistroEntity>

    @Query("DELETE FROM VariablesRegistros")
    suspend fun deleteAll()
}
