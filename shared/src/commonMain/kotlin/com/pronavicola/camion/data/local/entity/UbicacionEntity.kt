package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "Ubicacion",
    primaryKeys = ["IdVehiculo", "IdUbicacion"],
    foreignKeys = [
        ForeignKey(
            entity = VehiculoEntity::class,
            parentColumns = ["IdVehiculo"],
            childColumns = ["IdVehiculo"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["IdVehiculo"]),
        Index(value = ["IdDispositivo"]),
        Index(value = ["MarcaTiempo"]),
        Index(value = ["IdDispositivo", "MarcaTiempo"]),
    ],
)
data class UbicacionEntity(
    @ColumnInfo(name = "IdVehiculo") val idVehiculo: Int,
    @ColumnInfo(name = "IdUbicacion") val idUbicacion: Long = 0,
    @ColumnInfo(name = "IdDispositivo") val idDispositivo: String,
    @ColumnInfo(name = "MarcaTiempo") val marcaTiempo: Long, // Unix epoch milliseconds
    @ColumnInfo(name = "Latitud") val latitud: Double,
    @ColumnInfo(name = "Longitud") val longitud: Double,
    @ColumnInfo(name = "Altitud") val altitud: Double? = null,
    @ColumnInfo(name = "Precision") val precision: Float? = null,
    @ColumnInfo(name = "Velocidad") val velocidad: Float? = null,
)
