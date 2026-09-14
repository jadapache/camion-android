package com.pronavicola.camion.util

import android.content.Context
import android.os.Build
import android.provider.Settings

class AndroidPlatformInfoProvider(private val context: Context) : PlatformInfoProvider {
    override fun getHardwareFingerprint(): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!androidId.isNullOrBlank()) {
                "${Build.MANUFACTURER}_${Build.MODEL}_$androidId"
            } else {
                "${Build.MANUFACTURER}_${Build.MODEL}_${Build.BOARD}"
            }
        } catch (_: Exception) {
            "${Build.MANUFACTURER}_${Build.MODEL}_DEFAULT"
        }
    }

    override fun getDeviceBrand(): String = Build.MANUFACTURER
    override fun getDeviceModel(): String = Build.MODEL
    override fun getOsVersion(): String = "Android ${Build.VERSION.RELEASE}"
}

actual fun getPlatformInfoProvider(platformContext: Any?): PlatformInfoProvider {
    val context = platformContext as? Context ?: error("Context required for Android PlatformInfoProvider")
    return AndroidPlatformInfoProvider(context)
}
