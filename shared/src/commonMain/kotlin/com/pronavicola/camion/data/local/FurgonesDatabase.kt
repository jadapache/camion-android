package com.pronavicola.camion.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.pronavicola.camion.data.local.dao.AlarmaDao
import com.pronavicola.camion.data.local.dao.AlarmaRegistroDao
import com.pronavicola.camion.data.local.dao.AppConfigDao
import com.pronavicola.camion.data.local.dao.BuzonMqttDao
import com.pronavicola.camion.data.local.dao.DefinicionDao
import com.pronavicola.camion.data.local.dao.ParametroDao
import com.pronavicola.camion.data.local.dao.PerfilDao
import com.pronavicola.camion.data.local.dao.UbicacionDao
import com.pronavicola.camion.data.local.dao.UsuarioDao
import com.pronavicola.camion.data.local.dao.VariableRegistroDao
import com.pronavicola.camion.data.local.dao.VehiculoDao
import com.pronavicola.camion.data.local.entity.AlarmaEntity
import com.pronavicola.camion.data.local.entity.AlarmaRegistroEntity
import com.pronavicola.camion.data.local.entity.AlarmaRegistroView
import com.pronavicola.camion.data.local.entity.AppConfigEntity
import com.pronavicola.camion.data.local.entity.BuzonMqttEntity
import com.pronavicola.camion.data.local.entity.DefinicionEntity
import com.pronavicola.camion.data.local.entity.ParametroEntity
import com.pronavicola.camion.data.local.entity.PerfilEntity
import com.pronavicola.camion.data.local.entity.PerfilParametroEntity
import com.pronavicola.camion.data.local.entity.UbicacionEntity
import com.pronavicola.camion.data.local.entity.UsuarioEntity
import com.pronavicola.camion.data.local.entity.VariableEntity
import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.data.local.entity.VariablesRegistrosEntity
import com.pronavicola.camion.data.local.entity.VehiculoEntity

@Database(
    entities = [
        DefinicionEntity::class,
        PerfilEntity::class,
        UsuarioEntity::class,
        ParametroEntity::class,
        PerfilParametroEntity::class,
        VariableEntity::class,
        VariablesRegistrosEntity::class,
        AppConfigEntity::class,
        AlarmaEntity::class,
        AlarmaRegistroEntity::class,
        VehiculoEntity::class,
        UbicacionEntity::class,
        BuzonMqttEntity::class,
    ],
    views = [
        VariableRegistroEntity::class,
        AlarmaRegistroView::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(FurgonesDatabaseConstructor::class)
abstract class FurgonesDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun perfilDao(): PerfilDao
    abstract fun parametroDao(): ParametroDao
    abstract fun variableRegistroDao(): VariableRegistroDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun alarmaDao(): AlarmaDao
    abstract fun alarmaRegistroDao(): AlarmaRegistroDao
    abstract fun definicionDao(): DefinicionDao
    abstract fun vehiculoDao(): VehiculoDao
    abstract fun ubicacionDao(): UbicacionDao
    abstract fun buzonMqttDao(): BuzonMqttDao

    companion object {
        const val DB_NAME = "Furgones.db"
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object FurgonesDatabaseConstructor : RoomDatabaseConstructor<FurgonesDatabase> {
    override fun initialize(): FurgonesDatabase
}
