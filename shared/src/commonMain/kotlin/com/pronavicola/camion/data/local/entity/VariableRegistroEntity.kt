package com.pronavicola.camion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

@DatabaseView(
    viewName = "V_VariablesRegistro",
    value = """
        SELECT 
            ROW_NUMBER() OVER (ORDER BY vr.FechaHora ASC) AS IdRegistro,
            vr.IdVehiculo,
            COALESCE(MAX(v.Placa), '') AS Placa,
            vr.FechaHora AS Fecha,
            COALESCE(MAX(CASE WHEN vr.IdVariable = 1 THEN vr.Valor END), 0.0) AS Temperatura1,
            COALESCE(MAX(CASE WHEN vr.IdVariable = 2 THEN vr.Valor END), 0.0) AS Temperatura2,
            COALESCE(MAX(CASE WHEN vr.IdVariable = 4 THEN vr.Valor END), 0.0) AS Humedad,
            COALESCE(MAX(CASE WHEN vr.IdVariable = 5 THEN vr.Valor END), 0.0) AS Tension
        FROM VariablesRegistros vr
        LEFT JOIN Vehiculos v ON vr.IdVehiculo = v.IdVehiculo
        GROUP BY vr.IdVehiculo, vr.FechaHora
    """,
)
data class VariableRegistroEntity(
    @ColumnInfo(name = "IdRegistro") val idRegistro: Int = 0,
    @ColumnInfo(name = "IdVehiculo") val idVehiculo: Int = 1,
    @ColumnInfo(name = "Placa") val placa: String = "",
    @ColumnInfo(name = "Fecha") val fecha: String,
    @ColumnInfo(name = "Temperatura1") val temperatura1: Double,
    @ColumnInfo(name = "Temperatura2") val temperatura2: Double,
    @ColumnInfo(name = "Humedad") val humedad: Double,
    @ColumnInfo(name = "Tension") val tension: Double = 0.0,
)
