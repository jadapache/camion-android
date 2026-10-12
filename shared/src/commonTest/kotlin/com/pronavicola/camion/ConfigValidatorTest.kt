package com.pronavicola.camion

import com.pronavicola.camion.util.ConfigValidator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigValidatorTest {

    @Test
    fun testValidateDeviceId() {
        assertTrue(ConfigValidator.validateDeviceId("TRUCK-101"))
        assertTrue(ConfigValidator.validateDeviceId("uuid-1234-abcd"))
        assertFalse(ConfigValidator.validateDeviceId(""))
        assertFalse(ConfigValidator.validateDeviceId("   "))
        assertFalse(ConfigValidator.validateDeviceId(null))
    }

    @Test
    fun testValidateBrokerUrl() {
        assertTrue(ConfigValidator.validateBrokerUrl("tcp://mqtt.pronavicola.com:1883"))
        assertTrue(ConfigValidator.validateBrokerUrl("ssl://broker.emqx.io:8883"))
        assertTrue(ConfigValidator.validateBrokerUrl("ws://localhost:8083"))
        assertTrue(ConfigValidator.validateBrokerUrl("wss://test.mosquitto.org:8084"))
        assertTrue(ConfigValidator.validateBrokerUrl("tcp://192.168.1.100:1883"))

        assertFalse(ConfigValidator.validateBrokerUrl("http://mqtt.pronavicola.com:1883"))
        assertFalse(ConfigValidator.validateBrokerUrl("ftp://broker.com"))
        assertFalse(ConfigValidator.validateBrokerUrl("not-a-valid-url"))
        assertFalse(ConfigValidator.validateBrokerUrl(""))
        assertFalse(ConfigValidator.validateBrokerUrl(null))
    }

    @Test
    fun testValidateTopicRoot() {
        assertTrue(ConfigValidator.validateTopicRoot("furgones"))
        assertTrue(ConfigValidator.validateTopicRoot("pronavicola/furgones/v1"))
        assertFalse(ConfigValidator.validateTopicRoot("furgones/#"))
        assertFalse(ConfigValidator.validateTopicRoot("furgones/+/telemetry"))
        assertFalse(ConfigValidator.validateTopicRoot(""))
        assertFalse(ConfigValidator.validateTopicRoot(null))
    }

    @Test
    fun testValidateCoordinates() {
        assertTrue(ConfigValidator.validateCoordinates(4.570868, -74.297333)) // Colombia
        assertTrue(ConfigValidator.validateCoordinates(-33.4489, -70.6693)) // Santiago
        assertTrue(ConfigValidator.validateCoordinates(0.0, 0.0))
        assertTrue(ConfigValidator.validateCoordinates(90.0, 180.0))
        assertTrue(ConfigValidator.validateCoordinates(-90.0, -180.0))

        assertFalse(ConfigValidator.validateCoordinates(91.0, 0.0))
        assertFalse(ConfigValidator.validateCoordinates(-91.0, 0.0))
        assertFalse(ConfigValidator.validateCoordinates(0.0, 181.0))
        assertFalse(ConfigValidator.validateCoordinates(0.0, -181.0))
        assertFalse(ConfigValidator.validateCoordinates(null, 10.0))
        assertFalse(ConfigValidator.validateCoordinates(10.0, null))
    }

    @Test
    fun testValidateFurgonId() {
        assertTrue(ConfigValidator.validateFurgonId("FURGON-001"))
        assertFalse(ConfigValidator.validateFurgonId(""))
        assertFalse(ConfigValidator.validateFurgonId(null))
    }
}
