package com.pronavicola.camion.util

/**
 * Validates device configurations, MQTT URIs, topics, and GPS coordinates
 * per Requirements 18.1 - 18.7.
 */
object ConfigValidator {

    const val DEFAULT_BROKER_URL = "tcp://mqtt.pronavicola.com:1883"
    const val DEFAULT_TOPIC_ROOT = "furgones"

    fun validateDeviceId(deviceId: String?): Boolean {
        return !deviceId.isNullOrBlank()
    }

    fun validateBrokerUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val regex = Regex("^(tcp|ssl|ws|wss)://[\\w.-]+(:[0-9]+)?$")
        return regex.matches(url.trim())
    }

    fun validateTopicRoot(topic: String?): Boolean {
        if (topic.isNullOrBlank()) return false
        if (topic.length > 128) return false
        val invalidChars = charArrayOf('#', '+', '$', '\u0000')
        return topic.none { it in invalidChars }
    }

    fun validateLatitude(latitude: Double?): Boolean {
        return latitude != null && latitude in -90.0..90.0
    }

    fun validateLongitude(longitude: Double?): Boolean {
        return longitude != null && longitude in -180.0..180.0
    }

    fun validateCoordinates(latitude: Double?, longitude: Double?): Boolean {
        return validateLatitude(latitude) && validateLongitude(longitude)
    }

    fun validateFurgonId(furgonId: String?): Boolean {
        return !furgonId.isNullOrBlank()
    }
}
