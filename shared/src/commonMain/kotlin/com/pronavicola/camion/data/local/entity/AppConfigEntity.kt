package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "AppConfig")
data class AppConfigEntity(
    @PrimaryKey @ColumnInfo(name = "Clave") val clave: String,
    @ColumnInfo(name = "Valor") val valor: String,
)
