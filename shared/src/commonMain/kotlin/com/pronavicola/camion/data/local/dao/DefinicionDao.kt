package com.pronavicola.camion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pronavicola.camion.data.local.entity.DefinicionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DefinicionDao {
    @Query("SELECT * FROM Definiciones ORDER BY IdDefinicion ASC")
    fun observeAll(): Flow<List<DefinicionEntity>>

    @Query("SELECT * FROM Definiciones ORDER BY IdDefinicion ASC")
    suspend fun getAll(): List<DefinicionEntity>

    @Query("SELECT * FROM Definiciones WHERE IdDefinicion = :id LIMIT 1")
    suspend fun getById(id: String): DefinicionEntity?

    @Query("SELECT * FROM Definiciones WHERE IdDefinicion LIKE :prefix || '%' ORDER BY IdDefinicion ASC")
    suspend fun getByPrefix(prefix: String): List<DefinicionEntity>

    @Query("SELECT * FROM Definiciones WHERE IdDefinicion LIKE 'ESTAALAR.%' AND IdDefinicion != 'ESTAALAR.' ORDER BY IdDefinicion ASC")
    suspend fun getEstadosAlarmas(): List<DefinicionEntity>

    @Query("SELECT * FROM Definiciones WHERE IdDefinicion LIKE 'ESTAUSUA.%' AND IdDefinicion != 'ESTAUSUA.' ORDER BY IdDefinicion ASC")
    suspend fun getEstadosUsuarios(): List<DefinicionEntity>

    @Query("SELECT * FROM Definiciones WHERE IdDefinicion LIKE 'UNIDMEDI.%' AND IdDefinicion != 'UNIDMEDI.' ORDER BY IdDefinicion ASC")
    suspend fun getUnidadesMedida(): List<DefinicionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(definicion: DefinicionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(definiciones: List<DefinicionEntity>)

    @Query("DELETE FROM Definiciones")
    suspend fun deleteAll()
}
