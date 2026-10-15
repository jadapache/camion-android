package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "BuzonMqtt",
    indices = [
        Index(value = ["MarcaTiempo"]),
        Index(value = ["ConteoReintentos"]),
        Index(value = ["Estado"])
    ]
)
data class BuzonMqttEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,

    @ColumnInfo(name = "IdMensaje") val idMensaje: String, // UUID for idempotency

    @ColumnInfo(name = "Topico") val topico: String,

    @ColumnInfo(name = "CargaUtil") val cargaUtil: String, // JSON string

    @ColumnInfo(name = "Qos") val qos: Int = 1, // 0, 1, or 2

    @ColumnInfo(name = "MarcaTiempo") val marcaTiempo: Long, // Unix epoch milliseconds

    @ColumnInfo(name = "ConteoReintentos") val conteoReintentos: Int = 0,

    @ColumnInfo(name = "Estado") val estado: String = "PENDING", // PENDING, FAILED

    @ColumnInfo(name = "UltimoReintentoEn") val ultimoReintentoEn: Long? = null,

    @ColumnInfo(name = "MensajeError") val mensajeError: String? = null,
)
