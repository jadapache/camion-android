package com.pronavicola.camion.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pronavicola.camion.data.local.dao.ParametroDao
import com.pronavicola.camion.data.local.dao.PerfilDao
import com.pronavicola.camion.data.local.dao.UsuarioDao
import com.pronavicola.camion.data.local.entity.DefinicionEntity
import com.pronavicola.camion.data.local.entity.ParametroEntity
import com.pronavicola.camion.data.local.entity.PerfilEntity
import com.pronavicola.camion.data.local.entity.PerfilParametroEntity
import com.pronavicola.camion.data.local.entity.UsuarioEntity
import com.pronavicola.camion.data.local.entity.VariableEntity
import java.io.FileOutputStream

@Database(
    entities = [
        DefinicionEntity::class,
        PerfilEntity::class,
        UsuarioEntity::class,
        ParametroEntity::class,
        PerfilParametroEntity::class,
        VariableEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class FurgonesDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun perfilDao(): PerfilDao
    abstract fun parametroDao(): ParametroDao

    companion object {
        private const val DB_NAME = "Furgones.db"

        fun create(context: Context): FurgonesDatabase {
            copyAssetDatabaseIfNeeded(context)
            return Room.databaseBuilder(
                context.applicationContext,
                FurgonesDatabase::class.java,
                DB_NAME,
            ).build()
        }

        private fun copyAssetDatabaseIfNeeded(context: Context) {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (dbFile.exists()) return
            dbFile.parentFile?.mkdirs()
            context.assets.open(DB_NAME).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}
