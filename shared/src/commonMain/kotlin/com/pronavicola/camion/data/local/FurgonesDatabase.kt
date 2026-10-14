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
import com.pronavicola.camion.data.local.dao.TrazasUbicacionDao
import com.pronavicola.camion.data.local.dao.UsuarioDao
import com.pronavicola.camion.data.local.dao.VariableRegistroDao
import com.pronavicola.camion.data.local.dao.VehiculoDao
import com.pronavicola.camion.data.local.entity.AlarmaEntity
import com.pronavicola.camion.data.local.entity.AlarmaRegistroEntity
import com.pronavicola.camion.data.local.entity.AppConfigEntity
import com.pronavicola.camion.data.local.entity.BuzonMqttEntity
import com.pronavicola.camion.data.local.entity.DefinicionEntity
import com.pronavicola.camion.data.local.entity.ParametroEntity
import com.pronavicola.camion.data.local.entity.PerfilEntity
import com.pronavicola.camion.data.local.entity.PerfilParametroEntity
import com.pronavicola.camion.data.local.entity.TrazasUbicacionEntity
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
        TrazasUbicacionEntity::class,
        BuzonMqttEntity::class,
    ],
    views = [
        VariableRegistroEntity::class,
    ],
    version = 2,
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
    abstract fun trazasUbicacionDao(): TrazasUbicacionDao
    abstract fun buzonMqttDao(): BuzonMqttDao

    companion object {
        const val DB_NAME = "Furgones.db"
    }
}

val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
        connection.prepare("DROP VIEW IF EXISTS `V_VariablesRegistro`").use { it.step() }
        connection.prepare(
            """
                CREATE VIEW `V_VariablesRegistro` AS SELECT 
                    ROW_NUMBER() OVER (ORDER BY vr.FechaHora ASC) AS IdRegistro,
                    vr.IdVehiculo,
                    COALESCE(MAX(v.Placa), '') AS Placa,
                    vr.FechaHora AS Fecha,
                    COALESCE(MAX(CASE WHEN vr.IdVariable = 1 THEN vr.Valor END), 0.0) AS Temperatura1,
                    COALESCE(MAX(CASE WHEN vr.IdVariable = 2 THEN vr.Valor END), 0.0) AS Temperatura2,
                    COALESCE(MAX(CASE WHEN vr.IdVariable = 4 THEN vr.Valor END), 0.0) AS Humedad,
                    COALESCE(MAX(CASE WHEN vr.IdVariable = 5 THEN vr.Valor END), 0.0) AS Tension
                FROM VariablesRegistros vr
                LEFT JOIN Vehiculos v ON vr.IdVehiculo = v.IdVehiculo
                GROUP BY vr.IdVehiculo, vr.FechaHora
            """.trimIndent()
        ).use { it.step() }
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object FurgonesDatabaseConstructor : RoomDatabaseConstructor<FurgonesDatabase> {
    override fun initialize(): FurgonesDatabase
}
