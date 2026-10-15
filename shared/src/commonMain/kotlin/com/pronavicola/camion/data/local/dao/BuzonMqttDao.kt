package com.pronavicola.camion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pronavicola.camion.data.local.entity.BuzonMqttEntity

@Dao
interface BuzonMqttDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: BuzonMqttEntity): Long

    @Query("SELECT * FROM BuzonMqtt WHERE Estado = 'PENDING' ORDER BY MarcaTiempo ASC LIMIT :limit")
    suspend fun getPendingMessages(limit: Int = 50): List<BuzonMqttEntity>

    @Query("UPDATE BuzonMqtt SET ConteoReintentos = ConteoReintentos + 1, UltimoReintentoEn = :timestamp, MensajeError = :error WHERE id = :id")
    suspend fun updateRetryInfo(id: Long, timestamp: Long, error: String?)

    @Query("UPDATE BuzonMqtt SET Estado = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM BuzonMqtt WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM BuzonMqtt WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE BuzonMqtt SET Estado = 'FAILED' WHERE ConteoReintentos >= :maxRetries AND Estado = 'PENDING'")
    suspend fun markFailedAfterMaxRetries(maxRetries: Int = 10): Int

    @Query("SELECT COUNT(*) FROM BuzonMqtt WHERE Estado = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("DELETE FROM BuzonMqtt WHERE id IN (SELECT id FROM BuzonMqtt ORDER BY MarcaTiempo ASC LIMIT :count)")
    suspend fun pruneOldest(count: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM BuzonMqtt WHERE IdMensaje = :idMensaje)")
    suspend fun messageExists(idMensaje: String): Boolean
}
