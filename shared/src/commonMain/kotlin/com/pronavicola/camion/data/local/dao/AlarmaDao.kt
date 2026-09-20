package com.pronavicola.camion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pronavicola.camion.data.local.entity.AlarmaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmaDao {
    @Query("SELECT * FROM Alarmas ORDER BY IdAlarma ASC")
    fun observeAlarmas(): Flow<List<AlarmaEntity>>

    @Query("SELECT * FROM Alarmas ORDER BY IdAlarma ASC")
    suspend fun getAll(): List<AlarmaEntity>

    @Query("SELECT * FROM Alarmas WHERE IdAlarma = :idAlarma LIMIT 1")
    suspend fun getById(idAlarma: Int): AlarmaEntity?

    @Query("SELECT * FROM Alarmas WHERE Tag = :tag LIMIT 1")
    suspend fun getByTag(tag: String): AlarmaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarma: AlarmaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alarmas: List<AlarmaEntity>)

    @Query("SELECT COUNT(*) FROM Alarmas")
    suspend fun getCount(): Int
}
