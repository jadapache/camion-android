package com.pronavicola.camion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pronavicola.camion.data.local.entity.AppConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppConfigDao {
    @Query("SELECT Valor FROM AppConfig WHERE Clave = :clave LIMIT 1")
    suspend fun getValor(clave: String): String?

    @Query("SELECT Valor FROM AppConfig WHERE Clave = :clave LIMIT 1")
    fun observeValor(clave: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(config: AppConfigEntity)

    @Query("DELETE FROM AppConfig WHERE Clave = :clave")
    suspend fun deleteByClave(clave: String)
}
