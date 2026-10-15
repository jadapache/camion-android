package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "BandejaSalidaMqtt",
    indices = [
        Index(value = ["FechaHora"]),
        Index(value = ["Reintentos"]),
        Index(value = ["Estado"])
    ]
)
data class BandejaSalidaMqttEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "Id") val id: Long = 0,

    @ColumnInfo(name = "IdMensaje") val idMensaje: String, // UUID para idempotencia

    @ColumnInfo(name = "Topico") val topico: String,

    @ColumnInfo(name = "Payload") val payload: String, // JSON string

    @ColumnInfo(name = "Qos") val qos: Int = 1, // 0, 1, or 2

    @ColumnInfo(name = "FechaHora") val fechaHora: Long, // Unix epoch milliseconds

    @ColumnInfo(name = "Reintentos") val reintentos: Int = 0,

    @ColumnInfo(name = "Estado") val estado: String = "PENDIENTE", // PENDIENTE, FALLIDO

    @ColumnInfo(name = "UltimoReintento") val ultimoReintento: Long? = null,

    @ColumnInfo(name = "MensajeError") val mensajeError: String? = null
)
