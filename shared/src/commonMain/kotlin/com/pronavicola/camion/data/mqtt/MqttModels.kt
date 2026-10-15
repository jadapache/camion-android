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
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speedKmh: Double? = null,
    val altitudeM: Double? = null,
) {
    fun toJson(): String {
        val t1Str = temperatureC?.toString() ?: "null"
        val t2Str = temperature2C?.toString() ?: "null"
        val humStr = humidityPct?.toString() ?: "null"
        val vltStr = voltageV?.toString() ?: "null"
        val latStr = latitude?.toString() ?: "null"
        val lngStr = longitude?.toString() ?: "null"
        val speedStr = speedKmh?.toString() ?: "null"
        val altStr = altitudeM?.toString() ?: "null"

        val sb = StringBuilder()
        sb.append("{\"device_id\":\"").append(deviceId).append("\"")
        sb.append(",\"timestamp\":").append(timestamp)
        sb.append(",\"temperature_c\":").append(t1Str)
        sb.append(",\"temperature_2_c\":").append(t2Str)
        sb.append(",\"humidity_pct\":").append(humStr)
        sb.append(",\"voltage_v\":").append(vltStr)
        if (latitude != null && longitude != null) {
            sb.append(",\"latitude\":").append(latStr)
            sb.append(",\"longitude\":").append(lngStr)
            sb.append(",\"lat\":").append(latStr)
            sb.append(",\"lng\":").append(lngStr)
            sb.append(",\"speed\":").append(speedStr)
            sb.append(",\"speed_kmh\":").append(speedStr)
            sb.append(",\"altitude\":").append(altStr)
            sb.append(",\"altitude_m\":").append(altStr)
        }
        sb.append("}")
        return sb.toString()
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
        return "{\"device_id\":\"$deviceId\",\"latitude\":$latitude,\"longitude\":$longitude,\"lat\":$latitude,\"lng\":$longitude,\"speed\":$speedStr,\"speed_kmh\":$speedStr,\"altitude\":$altStr,\"altitude_m\":$altStr,\"timestamp\":$timestamp}"
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

