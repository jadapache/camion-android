package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "TrazasUbicacion",
    foreignKeys = [
        ForeignKey(
            entity = AppConfigEntity::class,
            parentColumns = ["Clave"],
            childColumns = ["IdDispositivo"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["IdDispositivo"]),
        Index(value = ["FechaHora"]),
        Index(value = ["IdDispositivo", "FechaHora"])
    ]
)
data class TrazasUbicacionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "Id") val id: Long = 0,

    @ColumnInfo(name = "IdDispositivo") val idDispositivo: String,

    @ColumnInfo(name = "FechaHora") val fechaHora: Long, // Unix epoch milliseconds

    @ColumnInfo(name = "Latitud") val latitud: Double,

    @ColumnInfo(name = "Longitud") val longitud: Double,

    @ColumnInfo(name = "Altitud") val altitud: Double? = null,

    @ColumnInfo(name = "Precision") val precision: Float? = null, // Metros

    @ColumnInfo(name = "Velocidad") val velocidad: Float? = null // Metros por segundo
)
