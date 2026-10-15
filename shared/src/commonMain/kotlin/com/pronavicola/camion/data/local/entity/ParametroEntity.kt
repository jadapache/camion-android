package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "Parametros",
    primaryKeys = ["IdParametro"],
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
    indices = [
        Index(value = ["IdTipo"]),
        Index(value = ["IdUnidad"]),
    ],
)
data class ParametroEntity(
    @ColumnInfo(name = "IdParametro") val idParametro: Int,
    @ColumnInfo(name = "Parametro") val parametro: String,
    @ColumnInfo(name = "IdTipo") val idTipo: String,
    @ColumnInfo(name = "Valor") val valor: Double,
    @ColumnInfo(name = "IdUnidad") val idUnidad: String,
    @ColumnInfo(name = "Tag") val tag: String,
    @ColumnInfo(name = "Minimo") val minimo: Double,
    @ColumnInfo(name = "Maximo") val maximo: Double,
)
