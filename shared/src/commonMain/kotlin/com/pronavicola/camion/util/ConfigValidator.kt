package com.pronavicola.camion.util

object ConfigValidator {

    fun validateDeviceId(deviceId: String?): Boolean {
        if (deviceId.isNullOrBlank()) return false
        return deviceId.length in 3..64
    }

    fun validateBrokerUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val clean = url.trim()
        return clean.isNotEmpty() && !clean.contains(" ")
    }

    fun validateTopicRoot(topicRoot: String?): Boolean {
        if (topicRoot.isNullOrBlank()) return false
        val clean = topicRoot.trim()
        if (clean.length > 128) return false
        // Wildcard characters are not allowed in base publish topics
        return !clean.contains("+") && !clean.contains("#")
    }

    fun validateCoordinates(lat: Double, lng: Double): Boolean {
        return lat in -90.0..90.0 && lng in -180.0..180.0
    }

    fun validateFurgonId(furgonId: String?): Boolean {
        return !furgonId.isNullOrBlank()
    }
}
