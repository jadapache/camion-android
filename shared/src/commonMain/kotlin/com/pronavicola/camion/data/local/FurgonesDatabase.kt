package com.pronavicola.camion.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
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
@ConstructedBy(FurgonesDatabaseConstructor::class)
abstract class FurgonesDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun perfilDao(): PerfilDao
    abstract fun parametroDao(): ParametroDao
    abstract fun variableRegistroDao(): VariableRegistroDao
    abstract fun appConfigDao(): AppConfigDao

    companion object {
        const val DB_NAME = "Furgones.db"

        fun getMigrations(): Array<Migration> {
            val list = mutableListOf<Migration>()
            for (from in 1..8) {
                list.add(object : Migration(from, 9) {
                    override fun migrate(connection: SQLiteConnection) {
                        createMissingTables(connection)
                    }
                })
            }
            return list.toTypedArray()
        }

        fun createMissingTables(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `Variables` (" +
                    "`IdVariable` INTEGER NOT NULL, " +
                    "`Variable` TEXT NOT NULL, " +
                    "`Minimo` REAL, " +
                    "`Maximo` REAL, " +
                    "`Estado` TEXT, " +
                    "PRIMARY KEY(`IdVariable`))"
            )
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `VariableRegistros` (" +
                    "`IdRegistro` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`IdVehiculo` TEXT NOT NULL, " +
                    "`Fecha` TEXT NOT NULL, " +
                    "`Temperatura1` REAL NOT NULL, " +
                    "`Temperatura2` REAL NOT NULL, " +
                    "`Humedad` REAL NOT NULL)"
            )
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `AppConfig` (" +
                    "`Clave` TEXT NOT NULL, " +
                    "`Valor` TEXT NOT NULL, " +
                    "PRIMARY KEY(`Clave`))"
            )
        }
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object FurgonesDatabaseConstructor : RoomDatabaseConstructor<FurgonesDatabase> {
    override fun initialize(): FurgonesDatabase
}
