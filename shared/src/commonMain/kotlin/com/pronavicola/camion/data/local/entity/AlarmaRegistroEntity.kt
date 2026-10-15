package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "AlarmasRegistros",
    primaryKeys = ["IdAlarma", "IdVehiculo", "IdViaje"],
    foreignKeys = [
        ForeignKey(
            entity = AlarmaEntity::class,
            parentColumns = ["IdAlarma"],
            childColumns = ["AlarmasIdAlarma"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = VehiculoEntity::class,
            parentColumns = ["IdVehiculo"],
            childColumns = ["VehiculosIdVehiculo"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = DefinicionEntity::class,
            parentColumns = ["IdDefinicion"],
            childColumns = ["DefinicionesIdDefinicion"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["AlarmasIdAlarma"]),
        Index(value = ["VehiculosIdVehiculo"]),
        Index(value = ["DefinicionesIdDefinicion"]),
    ],
)
data class AlarmaRegistroEntity(
    @ColumnInfo(name = "IdVehiculo") val idVehiculo: Int,
    @ColumnInfo(name = "IdAlarma") val idAlarma: Int,
    @ColumnInfo(name = "IdViaje") val idViaje: Int,
    @ColumnInfo(name = "HoraInicio") val horaInicio: String,
    @ColumnInfo(name = "HoraFin") val horaFin: String,
    @ColumnInfo(name = "Estado") val estado: String? = null,
    @ColumnInfo(name = "AlarmasIdAlarma") val alarmasIdAlarma: Int? = null,
    @ColumnInfo(name = "VehiculosIdVehiculo") val vehiculosIdVehiculo: Int? = null,
    @ColumnInfo(name = "DefinicionesIdDefinicion") val definicionesIdDefinicion: String? = null,
)
