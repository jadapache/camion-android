package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Variables")
data class VariableEntity(
    @PrimaryKey @ColumnInfo(name = "IdVariable") val idVariable: Int,
    @ColumnInfo(name = "Variable") val variable: String,
    @ColumnInfo(name = "Minimo") val minimo: Double?,
    @ColumnInfo(name = "Maximo") val maximo: Double?,
    @ColumnInfo(name = "Estado") val estado: String?,
)
