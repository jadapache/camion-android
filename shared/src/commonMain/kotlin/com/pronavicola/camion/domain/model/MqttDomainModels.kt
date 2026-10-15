package com.pronavicola.camion.domain.model

import java.util.UUID

data class MqttMessage(
    val topic: String,
    val payload: String,
    val qos: Int = 1,
    val messageId: String = UUID.randomUUID().toString()
)

data class TelemetryPayload(
    val device_id: String,
    val message_id: String,
    val timestamp: Long,
    val temperature_c: Double?,
    val temperature_2_c: Double?,
    val humidity_pct: Double?,
    val voltage_v: Double?,
    val proceso_activo: Boolean,
    val zonas: List<String>, // ["ON", "OFF", ...]
    val estado_irrigador: String, // "ON", "OFF", "OUT"
    val damper_activo: Boolean,
    val calefaccion_activa: Boolean,
    val alarmas: List<String>
) {
    fun toJson(): String {
        val t1Str = temperature_c?.toString() ?: "null"
        val t2Str = temperature_2_c?.toString() ?: "null"
        val humStr = humidity_pct?.toString() ?: "null"
        val vltStr = voltage_v?.toString() ?: "null"
        val zonasJson = zonas.joinToString(separator = ",", prefix = "[", postfix = "]") { "\"$it\"" }
        val alarmasJson = alarmas.joinToString(separator = ",", prefix = "[", postfix = "]") { "\"$it\"" }

        return """{"device_id":"$device_id","message_id":"$message_id","timestamp":$timestamp,"temperature_c":$t1Str,"temperature_2_c":$t2Str,"humidity_pct":$humStr,"voltage_v":$vltStr,"proceso_activo":$proceso_activo,"zonas":$zonasJson,"estado_irrigador":"$estado_irrigador","damper_activo":$damper_activo,"calefaccion_activa":$calefaccion_activa,"alarmas":$alarmasJson}"""
    }
}

enum class MqttConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
