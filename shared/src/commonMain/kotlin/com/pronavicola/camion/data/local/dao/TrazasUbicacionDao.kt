package com.pronavicola.camion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pronavicola.camion.data.local.entity.TrazasUbicacionEntity

@Dao
interface TrazasUbicacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(traza: TrazasUbicacionEntity): Long

    @Query("SELECT * FROM TrazasUbicacion WHERE FechaHora BETWEEN :startTime AND :endTime ORDER BY FechaHora ASC")
    suspend fun getTracesByTimeRange(startTime: Long, endTime: Long): List<TrazasUbicacionEntity>

    @Query("SELECT * FROM TrazasUbicacion ORDER BY FechaHora DESC LIMIT 1")
    suspend fun getLatestTrace(): TrazasUbicacionEntity?

    @Query("DELETE FROM TrazasUbicacion WHERE FechaHora < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long): Int

    @Query("SELECT COUNT(*) FROM TrazasUbicacion")
    suspend fun getTraceCount(): Int
}
