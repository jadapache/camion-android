package com.pronavicola.camion.domain.model

import com.pronavicola.camion.data.local.entity.TrazasUbicacionEntity

data class GpsLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val speed: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isValid(): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    fun toTrazasUbicacionEntity(deviceId: String): TrazasUbicacionEntity {
        return TrazasUbicacionEntity(
            idDispositivo = deviceId,
            fechaHora = timestamp,
            latitud = latitude,
            longitud = longitude,
            altitud = altitude,
            precision = accuracy,
            velocidad = speed
        )
    }
}

data class GpsLocationPayload(
    val device_id: String,
    val message_id: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val speed: Float? = null
) {
    fun toJson(): String {
        val altStr = altitude?.toString() ?: "null"
        val accStr = accuracy?.toString() ?: "null"
        val spdStr = speed?.toString() ?: "null"
        return """{"device_id":"$device_id","message_id":"$message_id","timestamp":$timestamp,"latitude":$latitude,"longitude":$longitude,"altitude":$altStr,"accuracy":$accStr,"speed":$spdStr}"""
    }
}
