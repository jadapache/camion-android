package com.pronavicola.camion.util

interface PlatformInfoProvider {
    fun getHardwareFingerprint(): String
    fun getDeviceBrand(): String
    fun getDeviceModel(): String
    fun getOsVersion(): String
    fun getAppVersion(): String = "1.0.0"
}

expect fun getPlatformInfoProvider(platformContext: Any? = null): PlatformInfoProvider
