package com.pronavicola.camion.data.mqtt

import com.pronavicola.camion.domain.model.ZoneStatus
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data structures and JSON serializations for MQTT payloads.
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
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("device_id", deviceId)
        json.put("timestamp", timestamp)
        json.put("temperature_c", temperatureC ?: JSONObject.NULL)
        json.put("temperature_2_c", temperature2C ?: JSONObject.NULL)
        json.put("humidity_pct", humidityPct ?: JSONObject.NULL)
        json.put("voltage_v", voltageV ?: JSONObject.NULL)
        json.put("proceso_activo", procesoActivo)

        val zonasArray = JSONArray()
        zonas.forEach { zonasArray.put(it.name) }
        json.put("zonas", zonasArray)
        json.put("estado_irrigador", estadoIrrigador.name)

        return json.toString()
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
        val json = JSONObject()
        json.put("device_id", deviceId)
        json.put("alarm_type", alarmType)
        json.put("severity", severity)
        json.put("trigger_value", triggerValue)
        json.put("triggered_at", triggeredAt)
        return json.toString()
    }
}

data class StatusMqttPayload(
    val deviceId: String,
    val status: String,
    val timestamp: Long = System.currentTimeMillis(),
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("device_id", deviceId)
        json.put("status", status)
        json.put("timestamp", timestamp)
        return json.toString()
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
