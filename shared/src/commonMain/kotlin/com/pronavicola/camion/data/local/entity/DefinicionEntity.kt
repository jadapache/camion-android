package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Definiciones")
data class DefinicionEntity(
    @PrimaryKey @ColumnInfo(name = "IdDefinicion") val idDefinicion: String,
    @ColumnInfo(name = "Definicion") val definicion: String,
)
