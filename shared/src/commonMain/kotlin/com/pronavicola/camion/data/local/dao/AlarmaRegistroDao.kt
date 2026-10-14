package com.pronavicola.camion.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pronavicola.camion.data.local.entity.AlarmaRegistroEntity
import com.pronavicola.camion.data.local.entity.AlarmaRegistroView
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmaRegistroDao {
    @Query("SELECT * FROM AlarmasRegistros ORDER BY HoraInicio DESC")
    fun observeAll(): Flow<List<AlarmaRegistroEntity>>

    @Query("SELECT * FROM AlarmasRegistros WHERE IdVehiculo = :idVehiculo ORDER BY HoraInicio DESC")
    fun observeByVehiculo(idVehiculo: Int): Flow<List<AlarmaRegistroEntity>>

    @Query("SELECT * FROM V_AlarmasRegistro ORDER BY HoraInicio DESC")
    fun observeAllViews(): Flow<List<AlarmaRegistroView>>

    @Query("SELECT * FROM V_AlarmasRegistro WHERE IdVehiculo = :idVehiculo ORDER BY HoraInicio DESC")
    fun observeViewsByVehiculo(idVehiculo: Int): Flow<List<AlarmaRegistroView>>

    @Query("SELECT * FROM AlarmasRegistros WHERE IdVehiculo = :idVehiculo AND IdViaje = :idViaje ORDER BY HoraInicio DESC")
    suspend fun getByVehiculoAndViaje(idVehiculo: Int, idViaje: Int): List<AlarmaRegistroEntity>

    @Query("SELECT * FROM AlarmasRegistros WHERE IdAlarma = :idAlarma AND IdVehiculo = :idVehiculo AND IdViaje = :idViaje LIMIT 1")
    suspend fun getById(idAlarma: Int, idVehiculo: Int, idViaje: Int): AlarmaRegistroEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(registro: AlarmaRegistroEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(registros: List<AlarmaRegistroEntity>)

    @Update
    suspend fun update(registro: AlarmaRegistroEntity)

    @Delete
    suspend fun delete(registro: AlarmaRegistroEntity)

    @Query("DELETE FROM AlarmasRegistros WHERE IdVehiculo = :idVehiculo AND IdViaje = :idViaje")
    suspend fun deleteByVehiculoAndViaje(idVehiculo: Int, idViaje: Int)

    @Query("DELETE FROM AlarmasRegistros")
    suspend fun deleteAll()
}
