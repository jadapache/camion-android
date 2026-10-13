package com.pronavicola.camion.data.mqtt

import com.pronavicola.camion.domain.model.ZoneStatus

/**
 * Standard topics designated for traces/telemetry, locations, alarms, and status.
 */
object MqttTopics {
    var topicRoot: String = "furgones"

    fun telemetry(deviceId: String): String = "$topicRoot/$deviceId/telemetry"
    fun location(deviceId: String): String = "$topicRoot/$deviceId/location"
    fun alarms(deviceId: String): String = "$topicRoot/$deviceId/alarms"
    fun status(deviceId: String): String = "$topicRoot/$deviceId/status"
}

/**
 * Data structures and JSON serializations for MQTT payloads.
 * Implemented with pure Kotlin formatting for high throughput and JVM testability.
 */
data class TelemetryMqttPayload(
    val deviceId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val temperatureC: Double?,
    val temperature2C: Double?,
    val humidityPct: Double?,
    val voltageV: Double?,
    val procesoActivo: Boolean,
    val zonas: List<ZoneStatus>,
    val estadoIrrigador: ZoneStatus,
    val damperActivo: Boolean = false,
    val calefaccionActiva: Boolean = false,
    val alarmas: List<String> = emptyList(),
) {
    fun toJson(): String {
        val t1Str = temperatureC?.toString() ?: "null"
        val t2Str = temperature2C?.toString() ?: "null"
        val humStr = humidityPct?.toString() ?: "null"
        val vltStr = voltageV?.toString() ?: "null"
        val zonasJson = zonas.joinToString(separator = ",", prefix = "[", postfix = "]") { "\"${it.name}\"" }
        val alarmasJson = alarmas.joinToString(separator = ",", prefix = "[", postfix = "]") { "\"$it\"" }

        return "{\"device_id\":\"$deviceId\",\"timestamp\":$timestamp,\"temperature_c\":$t1Str,\"temperature_2_c\":$t2Str,\"humidity_pct\":$humStr,\"voltage_v\":$vltStr,\"proceso_activo\":$procesoActivo,\"zonas\":$zonasJson,\"estado_irrigador\":\"${estadoIrrigador.name}\",\"damper_activo\":$damperActivo,\"calefaccion_activa\":$calefaccionActiva,\"alarmas\":$alarmasJson}"
    }
}

data class LocationMqttPayload(
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double? = null,
    val altitudeM: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
) {
    fun toJson(): String {
        val speedStr = speedKmh?.toString() ?: "null"
        val altStr = altitudeM?.toString() ?: "null"
        return "{\"device_id\":\"$deviceId\",\"latitude\":$latitude,\"longitude\":$longitude,\"speed_kmh\":$speedStr,\"altitude_m\":$altStr,\"timestamp\":$timestamp}"
    }
}

data class AlarmMqttPayload(
    val deviceId: String,
    val alarmType: String,
    val severity: String,
    val triggerValue: Double,
    val triggeredAt: Long = System.currentTimeMillis(),
) {
    fun toJson(): String {
        return "{\"device_id\":\"$deviceId\",\"alarm_type\":\"$alarmType\",\"severity\":\"$severity\",\"trigger_value\":$triggerValue,\"triggered_at\":$triggeredAt}"
    }
}

data class StatusMqttPayload(
    val deviceId: String,
    val status: String,
    val timestamp: Long = System.currentTimeMillis(),
) {
    fun toJson(): String {
        return "{\"device_id\":\"$deviceId\",\"status\":\"$status\",\"timestamp\":$timestamp}"
    }
}

data class OutboxMessage(
    val id: Long = System.nanoTime(),
    val topic: String,
    val payloadJson: String,
    val qos: Int = 1,
    var retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
