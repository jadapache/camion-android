package com.pronavicola.camion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pronavicola.camion.data.local.entity.VehiculoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehiculoDao {
    @Query("SELECT * FROM Vehiculos ORDER BY IdVehiculo ASC")
    fun observeAll(): Flow<List<VehiculoEntity>>

    @Query("SELECT * FROM Vehiculos WHERE Predeterminado = 1 LIMIT 1")
    fun observePredeterminado(): Flow<VehiculoEntity?>

    @Query("SELECT * FROM Vehiculos WHERE Predeterminado = 1 LIMIT 1")
    suspend fun getPredeterminado(): VehiculoEntity?

    @Query("SELECT * FROM Vehiculos WHERE IdVehiculo = :idVehiculo LIMIT 1")
    suspend fun getById(idVehiculo: Int): VehiculoEntity?

    @Query("SELECT * FROM Vehiculos WHERE UPPER(TRIM(Placa)) = UPPER(TRIM(:placa)) LIMIT 1")
    suspend fun getByPlaca(placa: String): VehiculoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vehiculo: VehiculoEntity): Long

    @Update
    suspend fun update(vehiculo: VehiculoEntity)

    @Query("DELETE FROM Vehiculos WHERE IdVehiculo = :idVehiculo")
    suspend fun deleteById(idVehiculo: Int)

    @Query("UPDATE Vehiculos SET Predeterminado = 0 WHERE Predeterminado = 1")
    suspend fun clearPredeterminado()

    @Query("UPDATE Vehiculos SET Predeterminado = 1 WHERE IdVehiculo = :idVehiculo")
    suspend fun setPredeterminado(idVehiculo: Int)
}
