package com.pronavicola.camion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pronavicola.camion.data.local.entity.UbicacionEntity

@Dao
interface UbicacionDao {
    @Query("SELECT COALESCE(MAX(IdUbicacion), 0) + 1 FROM Ubicacion WHERE IdVehiculo = :idVehiculo")
    suspend fun getNextIdUbicacion(idVehiculo: Int): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trace: UbicacionEntity): Long

    @Query("SELECT * FROM Ubicacion WHERE IdDispositivo = :idDispositivo AND MarcaTiempo >= :startTime AND MarcaTiempo <= :endTime ORDER BY MarcaTiempo DESC")
    suspend fun getTracesByTimeRange(
        idDispositivo: String,
        startTime: Long,
        endTime: Long,
    ): List<UbicacionEntity>

    @Query("SELECT * FROM Ubicacion WHERE IdVehiculo = :idVehiculo ORDER BY MarcaTiempo DESC LIMIT 1")
    suspend fun getLatestTraceByVehiculo(idVehiculo: Int): UbicacionEntity?

    @Query("SELECT * FROM Ubicacion WHERE IdDispositivo = :idDispositivo ORDER BY MarcaTiempo DESC LIMIT 1")
    suspend fun getLatestTrace(idDispositivo: String): UbicacionEntity?

    @Query("DELETE FROM Ubicacion WHERE MarcaTiempo < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long): Int

    @Query("SELECT COUNT(*) FROM Ubicacion")
    suspend fun getTraceCount(): Int
}
