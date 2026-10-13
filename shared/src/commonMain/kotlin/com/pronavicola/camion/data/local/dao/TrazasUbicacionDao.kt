package com.pronavicola.camion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pronavicola.camion.data.local.entity.TrazasUbicacionEntity

@Dao
interface TrazasUbicacionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trace: TrazasUbicacionEntity): Long

    @Query("SELECT * FROM TrazasUbicacion WHERE IdDispositivo = :idDispositivo AND MarcaTiempo >= :startTime AND MarcaTiempo <= :endTime ORDER BY MarcaTiempo DESC")
    suspend fun getTracesByTimeRange(
        idDispositivo: String,
        startTime: Long,
        endTime: Long,
    ): List<TrazasUbicacionEntity>

    @Query("SELECT * FROM TrazasUbicacion WHERE IdDispositivo = :idDispositivo ORDER BY MarcaTiempo DESC LIMIT 1")
    suspend fun getLatestTrace(idDispositivo: String): TrazasUbicacionEntity?

    @Query("DELETE FROM TrazasUbicacion WHERE MarcaTiempo < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long): Int

    @Query("SELECT COUNT(*) FROM TrazasUbicacion")
    suspend fun getTraceCount(): Int
}
