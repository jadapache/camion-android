package com.pronavicola.camion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pronavicola.camion.data.local.entity.PerfilEntity

@Dao
interface PerfilDao {
    @Query("SELECT * FROM Perfiles WHERE IdPerfil = :idPerfil LIMIT 1")
    suspend fun getById(idPerfil: Int): PerfilEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(perfil: PerfilEntity)
}
