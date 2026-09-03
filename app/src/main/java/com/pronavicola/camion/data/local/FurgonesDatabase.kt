package com.pronavicola.camion.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pronavicola.camion.data.local.dao.ParametroDao
import com.pronavicola.camion.data.local.dao.PerfilDao
import com.pronavicola.camion.data.local.dao.UsuarioDao
import com.pronavicola.camion.data.local.dao.VariableRegistroDao
import com.pronavicola.camion.data.local.dao.AppConfigDao
import com.pronavicola.camion.data.local.entity.DefinicionEntity
import com.pronavicola.camion.data.local.entity.ParametroEntity
import com.pronavicola.camion.data.local.entity.PerfilEntity
import com.pronavicola.camion.data.local.entity.PerfilParametroEntity
import com.pronavicola.camion.data.local.entity.UsuarioEntity
import com.pronavicola.camion.data.local.entity.VariableEntity
import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.data.local.entity.AppConfigEntity

@Database(
    entities = [
        DefinicionEntity::class,
        PerfilEntity::class,
        UsuarioEntity::class,
        ParametroEntity::class,
        PerfilParametroEntity::class,
        VariableEntity::class,
        VariableRegistroEntity::class,
        AppConfigEntity::class,
    ],
    version = 9,
    exportSchema = false,
)
abstract class FurgonesDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun perfilDao(): PerfilDao
    abstract fun parametroDao(): ParametroDao
    abstract fun variableRegistroDao(): VariableRegistroDao
    abstract fun appConfigDao(): AppConfigDao

    companion object {
        private const val DB_NAME = "Furgones.db"

        fun create(context: Context): FurgonesDatabase {
            val migration3to4to5 = object : Migration(3, 5) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    createMissingTables(database)
                }
            }

            val migration4to5 = object : Migration(4, 5) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    createMissingTables(database)
                }
            }

            val migration5to6 = object : Migration(5, 6) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    createMissingTables(database)
                }
            }

            val migration6to7 = object : Migration(6, 7) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    createMissingTables(database)
                }
            }

            val migration7to8 = object : Migration(7, 8) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    createMissingTables(database)
                }
            }

            val migration8to9 = object : Migration(8, 9) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    createMissingTables(database)
                }
            }

            // Comprehensive migrations to avoid destructive migration loops for all possible previous versions
            val oldMigrations = (1..8).map { from ->
                object : Migration(from, 9) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        createMissingTables(database)
                    }
                }
            }

            return Room.databaseBuilder(
                context.applicationContext,
                FurgonesDatabase::class.java,
                DB_NAME,
            )
                .createFromAsset(DB_NAME)
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .addMigrations(migration3to4to5, migration4to5, migration5to6, migration6to7, migration7to8, migration8to9)
                .addMigrations(*oldMigrations.toTypedArray())
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        createMissingTables(db)
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
        }

        private fun createMissingTables(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `Variables` (" +
                    "`IdVariable` INTEGER NOT NULL, " +
                    "`Variable` TEXT NOT NULL, " +
                    "`Minimo` REAL, " +
                    "`Maximo` REAL, " +
                    "`Estado` TEXT, " +
                    "PRIMARY KEY(`IdVariable`))"
            )
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `VariableRegistros` (" +
                    "`IdRegistro` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`IdVehiculo` TEXT NOT NULL, " +
                    "`Fecha` TEXT NOT NULL, " +
                    "`Temperatura1` REAL NOT NULL, " +
                    "`Temperatura2` REAL NOT NULL, " +
                    "`Humedad` REAL NOT NULL)"
            )
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `AppConfig` (" +
                    "`Clave` TEXT NOT NULL, " +
                    "`Valor` TEXT NOT NULL, " +
                    "PRIMARY KEY(`Clave`))"
            )
        }
    }
}
