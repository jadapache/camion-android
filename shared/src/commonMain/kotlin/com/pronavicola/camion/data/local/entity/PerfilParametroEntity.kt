package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "PerfilesParametros",
    primaryKeys = ["IdPerfil", "IdParametro"],
    foreignKeys = [
        ForeignKey(
            entity = PerfilEntity::class,
            parentColumns = ["IdPerfil"],
            childColumns = ["IdPerfil"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index(value = ["IdPerfil"]), Index(value = ["IdParametro"])],
)
data class PerfilParametroEntity(
    @ColumnInfo(name = "IdPerfil") val idPerfil: Int,
    @ColumnInfo(name = "IdParametro") val idParametro: Int,
)
