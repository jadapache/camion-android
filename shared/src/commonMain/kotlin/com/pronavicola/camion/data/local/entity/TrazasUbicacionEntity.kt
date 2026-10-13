package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "TrazasUbicacion",
    indices = [
        Index(value = ["IdDispositivo"]),
        Index(value = ["MarcaTiempo"]),
        Index(value = ["IdDispositivo", "MarcaTiempo"])
    ]
)
data class TrazasUbicacionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,

    @ColumnInfo(name = "IdDispositivo") val idDispositivo: String,

    @ColumnInfo(name = "MarcaTiempo") val marcaTiempo: Long, // Unix epoch milliseconds

    @ColumnInfo(name = "Latitud") val latitud: Double,

    @ColumnInfo(name = "Longitud") val longitud: Double,

    @ColumnInfo(name = "Altitud") val altitud: Double? = null,

    @ColumnInfo(name = "Precision") val precision: Float? = null,

    @ColumnInfo(name = "Velocidad") val velocidad: Float? = null,
)
