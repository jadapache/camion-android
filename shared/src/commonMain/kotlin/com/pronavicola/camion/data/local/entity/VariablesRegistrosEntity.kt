package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "VariablesRegistros",
    primaryKeys = ["IdVehiculo", "IdVariable", "FechaHora"],
    foreignKeys = [
        ForeignKey(
            entity = VehiculoEntity::class,
            parentColumns = ["IdVehiculo"],
            childColumns = ["IdVehiculo"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = VariableEntity::class,
            parentColumns = ["IdVariable"],
            childColumns = ["IdVariable"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["IdVehiculo"]),
        Index(value = ["IdVariable"]),
    ],
)
data class VariablesRegistrosEntity(
    @ColumnInfo(name = "IdVehiculo") val idVehiculo: Int,
    @ColumnInfo(name = "IdVariable") val idVariable: Int,
    @ColumnInfo(name = "FechaHora") val fechaHora: String,
    @ColumnInfo(name = "Valor") val valor: Double,
    @ColumnInfo(name = "Estado") val estado: String? = "PENDIENTE",
)
