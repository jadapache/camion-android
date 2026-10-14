package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

@DatabaseView(
    viewName = "V_AlarmasRegistro",
    value = """
        SELECT 
            ar.IdAlarma,
            ar.IdVehiculo,
            ar.IdViaje,
            COALESCE(v.Placa, '') AS Placa,
            COALESCE(a.Alarma, '') AS Alarma,
            ar.HoraInicio,
            ar.HoraFin,
            ar.Estado,
            ar.AlarmasIdAlarma,
            ar.VehiculosIdVehiculo,
            ar.DefinicionesIdDefinicion
        FROM AlarmasRegistros ar
        LEFT JOIN Vehiculos v ON ar.IdVehiculo = v.IdVehiculo
        LEFT JOIN Alarmas a ON ar.IdAlarma = a.IdAlarma
    """,
)
data class AlarmaRegistroView(
    @ColumnInfo(name = "IdAlarma") val idAlarma: Int,
    @ColumnInfo(name = "IdVehiculo") val idVehiculo: Int,
    @ColumnInfo(name = "IdViaje") val idViaje: Int,
    @ColumnInfo(name = "Placa") val placa: String = "",
    @ColumnInfo(name = "Alarma") val alarma: String = "",
    @ColumnInfo(name = "HoraInicio") val horaInicio: String,
    @ColumnInfo(name = "HoraFin") val horaFin: String,
    @ColumnInfo(name = "Estado") val estado: String? = null,
    @ColumnInfo(name = "AlarmasIdAlarma") val alarmasIdAlarma: Int? = null,
    @ColumnInfo(name = "VehiculosIdVehiculo") val vehiculosIdVehiculo: Int? = null,
    @ColumnInfo(name = "DefinicionesIdDefinicion") val definicionesIdDefinicion: String? = null,
)
