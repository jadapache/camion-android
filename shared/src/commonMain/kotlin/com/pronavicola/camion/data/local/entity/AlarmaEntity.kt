package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Alarmas")
data class AlarmaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "IdAlarma")
    val idAlarma: Int = 0,
    @ColumnInfo(name = "Alarma")
    val alarma: String? = null,
    @ColumnInfo(name = "Tag")
    val tag: String? = null,
    @ColumnInfo(name = "Descripcion")
    val descripcion: String? = null,
)
