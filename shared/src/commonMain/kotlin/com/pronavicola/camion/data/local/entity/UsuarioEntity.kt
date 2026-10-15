package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    @ColumnInfo(name = "Email") val email: String?,
    @ColumnInfo(name = "Estado") val estado: String,
)
