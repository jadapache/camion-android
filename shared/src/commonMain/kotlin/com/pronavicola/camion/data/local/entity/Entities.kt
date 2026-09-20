package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "Definiciones")
data class DefinicionEntity(
    @PrimaryKey @ColumnInfo(name = "IdDefinicion") val idDefinicion: String,
    @ColumnInfo(name = "Definicion") val definicion: String,
)

@Entity(tableName = "Perfiles")
data class PerfilEntity(
    @PrimaryKey @ColumnInfo(name = "IdPerfil") val idPerfil: Int,
    @ColumnInfo(name = "Perfil") val perfil: String,
)

@Entity(
    tableName = "Usuarios",
    foreignKeys = [
        ForeignKey(
            entity = PerfilEntity::class,
            parentColumns = ["IdPerfil"],
            childColumns = ["IdPerfil"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index(value = ["IdPerfil"])],
)
data class UsuarioEntity(
    @PrimaryKey @ColumnInfo(name = "IdUsuario") val idUsuario: String,
    @ColumnInfo(name = "Usuario") val usuario: String,
    @ColumnInfo(name = "Password") val password: String,
    @ColumnInfo(name = "IdPerfil") val idPerfil: Int,
    @ColumnInfo(name = "IdVehiculo") val idVehiculo: String?,
    @ColumnInfo(name = "Email") val email: String?,
    @ColumnInfo(name = "Estado") val estado: String,
)

@Entity(
    tableName = "Parametros",
    primaryKeys = ["IdVehiculo", "IdParametro"],
    foreignKeys = [
        ForeignKey(
            entity = DefinicionEntity::class,
            parentColumns = ["IdDefinicion"],
            childColumns = ["IdTipo"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = DefinicionEntity::class,
            parentColumns = ["IdDefinicion"],
            childColumns = ["IdUnidad"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index(value = ["IdTipo"]), Index(value = ["IdUnidad"])],
)
data class ParametroEntity(
    @ColumnInfo(name = "IdVehiculo") val idVehiculo: String,
    @ColumnInfo(name = "IdParametro") val idParametro: Int,
    @ColumnInfo(name = "Parametro") val parametro: String,
    @ColumnInfo(name = "IdTipo") val idTipo: String,
    @ColumnInfo(name = "Valor") val valor: Double,
    @ColumnInfo(name = "IdUnidad") val idUnidad: String,
    @ColumnInfo(name = "Tag") val tag: String,
    @ColumnInfo(name = "Minimo") val minimo: Double,
    @ColumnInfo(name = "Maximo") val maximo: Double,
)

@Entity(
    tableName = "PerfilesParametros",
    primaryKeys = ["IdPerfil", "IdVehiculo", "IdParametro"],
    foreignKeys = [
        ForeignKey(
            entity = PerfilEntity::class,
            parentColumns = ["IdPerfil"],
            childColumns = ["IdPerfil"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = ParametroEntity::class,
            parentColumns = ["IdVehiculo", "IdParametro"],
            childColumns = ["IdVehiculo", "IdParametro"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index(value = ["IdVehiculo", "IdParametro"])],
)
data class PerfilParametroEntity(
    @ColumnInfo(name = "IdPerfil") val idPerfil: Int,
    @ColumnInfo(name = "IdVehiculo") val idVehiculo: String,
    @ColumnInfo(name = "IdParametro") val idParametro: Int,
)

@Entity(tableName = "Variables")
data class VariableEntity(
    @PrimaryKey @ColumnInfo(name = "IdVariable") val idVariable: Int,
    @ColumnInfo(name = "Variable") val variable: String,
    @ColumnInfo(name = "Minimo") val minimo: Double?,
    @ColumnInfo(name = "Maximo") val maximo: Double?,
    @ColumnInfo(name = "Estado") val estado: String?,
)

@Entity(tableName = "VariableRegistros")
data class VariableRegistroEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "IdRegistro") val idRegistro: Int = 0,
    @ColumnInfo(name = "IdVehiculo") val idVehiculo: String,
    @ColumnInfo(name = "Fecha") val fecha: String,
    @ColumnInfo(name = "Temperatura1") val temperatura1: Double,
    @ColumnInfo(name = "Temperatura2") val temperatura2: Double,
    @ColumnInfo(name = "Humedad") val humedad: Double,
    @ColumnInfo(name = "EstadoEnvio") val estadoEnvio: String = "PENDIENTE",
)

@Entity(tableName = "AppConfig")
data class AppConfigEntity(
    @PrimaryKey @ColumnInfo(name = "Clave") val clave: String,
    @ColumnInfo(name = "Valor") val valor: String,
)

