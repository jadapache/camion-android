package com.pronavicola.camion.data.mqtt

/**
 * Standard topics designated for traces/telemetry, locations, and alarms.
 */
object MqttTopics {
    var topicRoot: String = "furgones"

    fun telemetry(deviceId: String): String = "$topicRoot/$deviceId/telemetry"
    fun location(deviceId: String): String = "$topicRoot/$deviceId/location"
    fun alarms(deviceId: String): String = "$topicRoot/$deviceId/alarms"
}

/**
 * Data structures and JSON serializations for MQTT payloads.
 * Telemetry consists solely of physical variables: Temperatura1, Temperatura2, Humedad, Tension.
 */
data class TelemetryMqttPayload(
    val deviceId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val temperatureC: Double?,
    val temperature2C: Double?,
    val humidityPct: Double?,
    val voltageV: Double?,
) {
    fun toJson(): String {
        val t1Str = temperatureC?.toString() ?: "null"
        val t2Str = temperature2C?.toString() ?: "null"
        val humStr = humidityPct?.toString() ?: "null"
        val vltStr = voltageV?.toString() ?: "null"

        return "{\"device_id\":\"$deviceId\",\"timestamp\":$timestamp,\"temperature_c\":$t1Str,\"temperature_2_c\":$t2Str,\"humidity_pct\":$humStr,\"voltage_v\":$vltStr}"
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
    val severity: String = "WARNING",
    val triggerValue: Double,
    val triggeredAt: Long = System.currentTimeMillis(),
) {
    fun toJson(): String {
        return "{\"device_id\":\"$deviceId\",\"alarm_type\":\"$alarmType\",\"severity\":\"$severity\",\"trigger_value\":$triggerValue,\"triggered_at\":$triggeredAt}"
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

