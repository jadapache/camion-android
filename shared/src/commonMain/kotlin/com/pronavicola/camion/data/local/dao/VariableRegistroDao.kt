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
    @Query("SELECT * FROM V_VariablesRegistro WHERE IdVehiculo = :idVehiculo ORDER BY Fecha DESC")
    fun observeRegistros(idVehiculo: Int): Flow<List<VariableRegistroEntity>>

    @Query("SELECT * FROM V_VariablesRegistro WHERE IdVehiculo = :idVehiculo AND UPPER(EstadoEnvio) != 'ENVIADO' ORDER BY Fecha ASC")
    suspend fun getPendingRegistros(idVehiculo: Int): List<VariableRegistroEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaw(registro: VariablesRegistrosEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawAll(registros: List<VariablesRegistrosEntity>)

    @Query("UPDATE VariablesRegistros SET Estado = :estado WHERE FechaHora = (SELECT Fecha FROM V_VariablesRegistro WHERE IdRegistro = :idRegistro LIMIT 1)")
    suspend fun updateEstadoEnvio(idRegistro: Int, estado: String)

    @Query("UPDATE VariablesRegistros SET Estado = :status WHERE FechaHora = (SELECT Fecha FROM V_VariablesRegistro WHERE IdRegistro = :recordId LIMIT 1)")
    suspend fun updateEstado(recordId: Long, status: String)

    @Query("UPDATE VariablesRegistros SET Estado = :estado WHERE FechaHora = :fecha")
    suspend fun updateEstadoEnvioByFecha(fecha: String, estado: String)

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
