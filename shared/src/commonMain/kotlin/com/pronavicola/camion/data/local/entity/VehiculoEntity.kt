package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Vehiculos")
data class VehiculoEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "IdVehiculo")
    val idVehiculo: Int = 0,
    @ColumnInfo(name = "Placa")
    val placa: String,
    @ColumnInfo(name = "Predeterminado")
    val predeterminado: Int = 0,
)
