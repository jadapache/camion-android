package com.pronavicola.camion

import com.pronavicola.camion.data.mqtt.LocationMqttPayload
import com.pronavicola.camion.domain.model.GpsLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GpsLocationValidationTest {

    @Test
    fun testGpsLocationCoordinatesValidation() {
        val validBogota = GpsLocation(latitud = 4.7110, longitud = -74.0721)
        assertTrue(validBogota.isValid())

        val invalidLat = GpsLocation(latitud = 95.0, longitud = -74.0721)
        assertFalse(invalidLat.isValid())

        val invalidLng = GpsLocation(latitud = 4.7110, longitud = 185.0)
        assertFalse(invalidLng.isValid())
    }

    @Test
    fun testGpsLocationToEntityConversion() {
        val location = GpsLocation(
            latitud = 7.1254,
            longitud = -73.1198,
            altitud = 959.0,
            precision = 4.5f,
            velocidad = 45.0f,
            marcaTiempo = 1760000000000L,
        )

        val entity = location.toUbicacionEntity(
            idVehiculo = 1,
            deviceId = "CAMION-01",
            idUbicacion = 42L,
        )
        assertEquals(1, entity.idVehiculo)
        assertEquals(42L, entity.idUbicacion)
        assertEquals("CAMION-01", entity.idDispositivo)
        assertEquals(7.1254, entity.latitud)
        assertEquals(-73.1198, entity.longitud)
        assertEquals(959.0, entity.altitud)
        assertEquals(4.5f, entity.precision)
        assertEquals(45.0f, entity.velocidad)
        assertEquals(1760000000000L, entity.marcaTiempo)
    }

    @Test
    fun testLocationMqttPayloadJsonSerialization() {
        val payload = LocationMqttPayload(
            deviceId = "ABC123",
            latitude = 4.7110,
            longitude = -74.0721,
            speedKmh = 55.4,
            altitudeM = 2600.0,
            timestamp = 1760000000000L,
        )

        val json = payload.toJson()
        assertTrue(json.contains("\"device_id\":\"ABC123\""))
        assertTrue(json.contains("\"latitude\":4.711"))
        assertTrue(json.contains("\"longitude\":-74.0721"))
        assertTrue(json.contains("\"speed_kmh\":55.4"))
    }
}
