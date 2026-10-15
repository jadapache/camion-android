package com.pronavicola.camion.domain.model

import com.pronavicola.camion.data.local.entity.UbicacionEntity

data class GpsLocation(
    val latitud: Double,
    val longitud: Double,
    val altitud: Double? = null,
    val precision: Float? = null,
    val velocidad: Float? = null,
    val marcaTiempo: Long = System.currentTimeMillis(),
) {
    fun isValid(): Boolean {
        return latitud in -90.0..90.0 && longitud in -180.0..180.0
    }

    fun toUbicacionEntity(
        idVehiculo: Int,
        deviceId: String,
        idUbicacion: Long = 0,
    ): UbicacionEntity {
        return UbicacionEntity(
            idVehiculo = idVehiculo,
            idUbicacion = idUbicacion,
            idDispositivo = deviceId,
            marcaTiempo = marcaTiempo,
            latitud = latitud,
            longitud = longitud,
            altitud = altitud,
            precision = precision,
            velocidad = velocidad,
        )
    }
}
