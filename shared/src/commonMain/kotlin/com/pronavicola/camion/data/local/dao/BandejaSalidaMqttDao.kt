package com.pronavicola.camion.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pronavicola.camion.data.local.entity.BandejaSalidaMqttEntity

@Dao
interface BandejaSalidaMqttDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mensaje: BandejaSalidaMqttEntity): Long

    @Query("SELECT * FROM BandejaSalidaMqtt WHERE Estado = 'PENDIENTE' ORDER BY FechaHora ASC LIMIT :limit")
    suspend fun getPendingMessages(limit: Int = 100): List<BandejaSalidaMqttEntity>

    @Query("UPDATE BandejaSalidaMqtt SET Reintentos = :reintentos, UltimoReintento = :timestamp, MensajeError = :error WHERE Id = :id")
    suspend fun updateRetryInfo(id: Long, reintentos: Int, timestamp: Long, error: String?)

    @Query("UPDATE BandejaSalidaMqtt SET Estado = :estado WHERE Id = :id")
    suspend fun updateStatus(id: Long, estado: String)

    @Delete
    suspend fun delete(mensaje: BandejaSalidaMqttEntity)

    @Query("DELETE FROM BandejaSalidaMqtt WHERE Id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE BandejaSalidaMqtt SET Estado = 'FALLIDO' WHERE Reintentos >= :maxRetries AND Estado = 'PENDIENTE'")
    suspend fun markFailedAfterMaxRetries(maxRetries: Int = 10): Int

    @Query("SELECT COUNT(*) FROM BandejaSalidaMqtt WHERE Estado = 'PENDIENTE'")
    suspend fun getPendingCount(): Int

    @Query("DELETE FROM BandejaSalidaMqtt WHERE Id IN (SELECT Id FROM BandejaSalidaMqtt ORDER BY FechaHora ASC LIMIT (MAX(0, (SELECT COUNT(*) FROM BandejaSalidaMqtt) - :maxSize)))")
    suspend fun pruneToSize(maxSize: Int = 10000): Int

    @Query("SELECT EXISTS(SELECT 1 FROM BandejaSalidaMqtt WHERE IdMensaje = :idMensaje LIMIT 1)")
    suspend fun messageExists(idMensaje: String): Boolean
}
