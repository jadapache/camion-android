package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Perfiles")
data class PerfilEntity(
    @PrimaryKey @ColumnInfo(name = "IdPerfil") val idPerfil: Int,
    @ColumnInfo(name = "Perfil") val perfil: String,
)
