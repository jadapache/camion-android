package com.pronavicola.camion.domain.model

data class DeviceConfiguration(
    val deviceId: String,
    val deviceToken: String? = null,
    val tokenExpiry: Long? = null,
    val furgonId: String? = null,
    val furgonPlate: String? = null,
    val brokerUrl: String = "furgones.emqx.io:1883",
    val topicRoot: String = "furgones",
    val gpsPublishIntervalSec: Long = 120,
    val telemetryPublishIntervalSec: Long = 30,
)
