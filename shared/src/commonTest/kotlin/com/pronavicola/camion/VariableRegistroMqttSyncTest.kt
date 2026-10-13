package com.pronavicola.camion

import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.data.mqtt.TelemetryMqttPayload
import kotlin.test.Test
import kotlin.test.assertTrue

class VariableRegistroMqttSyncTest {

    @Test
    fun testVariableRegistroToJsonConversion() {
        val registro = VariableRegistroEntity(
            idRegistro = 10,
            idVehiculo = 1,
            fecha = "2026-09-01 14:30:00",
            temperatura1 = 14.5,
            temperatura2 = 15.2,
            humedad = 65.0,
            tension = 24.1,
        )

        val payload = TelemetryMqttPayload(
            deviceId = "ABC123",
            timestamp = 1788273000000L,
            temperatureC = registro.temperatura1,
            temperature2C = registro.temperatura2,
            humidityPct = registro.humedad,
            voltageV = registro.tension,
        )

        val jsonStr = payload.toJson()

        assertTrue(jsonStr.contains("\"device_id\":\"ABC123\""))
        assertTrue(jsonStr.contains("\"temperature_c\":14.5"))
        assertTrue(jsonStr.contains("\"temperature_2_c\":15.2"))
        assertTrue(jsonStr.contains("\"humidity_pct\":65.0"))
        assertTrue(jsonStr.contains("\"voltage_v\":24.1"))
    }
}
