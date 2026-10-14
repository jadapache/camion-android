package com.pronavicola.camion.util

import org.json.JSONObject
import java.io.File

class DesktopPersistentDeviceIdentityStore : PersistentDeviceIdentityStore {
    private val tag = "DesktopDeviceIdentityStore"
    private val storeFile by lazy {
        val dir = File(System.getProperty("user.home"), ".camion")
        if (!dir.exists()) dir.mkdirs()
        File(dir, ".camion_device_identity.json")
    }

    override fun saveIdentity(identity: StoredDeviceIdentity) {
        if (identity.deviceId.isBlank()) return
        try {
            val json = JSONObject().apply {
                put("device_id", identity.deviceId)
                put("license_key", identity.licenseKey ?: "")
                put("server_base_url", identity.serverBaseUrl ?: "")
                put("furgon_plate", identity.furgonPlate ?: "")
                put("device_token", identity.deviceToken ?: "")
                put("updated_at", System.currentTimeMillis())
            }.toString()
            storeFile.writeText(json)
        } catch (e: Exception) {
            Log.w(tag, "Could not save desktop identity: ${e.message}")
        }
    }

    override fun loadIdentity(): StoredDeviceIdentity? {
        return try {
            if (storeFile.exists() && storeFile.canRead()) {
                val text = storeFile.readText().trim()
                if (text.isNotBlank()) {
                    val obj = JSONObject(text)
                    val devId = obj.optString("device_id", "").trim()
                    if (devId.isNotBlank()) {
                        StoredDeviceIdentity(
                            deviceId = devId,
                            licenseKey = obj.optString("license_key", "").takeIf { it.isNotBlank() },
                            serverBaseUrl = obj.optString("server_base_url", "").takeIf { it.isNotBlank() },
                            furgonPlate = obj.optString("furgon_plate", "").takeIf { it.isNotBlank() },
                            deviceToken = obj.optString("device_token", "").takeIf { it.isNotBlank() },
                        )
                    } else null
                } else null
            } else null
        } catch (e: Exception) {
            Log.w(tag, "Could not load desktop identity: ${e.message}")
            null
        }
    }

    override fun clearIdentity() {
        try {
            if (storeFile.exists()) storeFile.delete()
        } catch (_: Exception) {}
    }
}

actual fun getPersistentDeviceIdentityStore(platformContext: Any?): PersistentDeviceIdentityStore {
    return DesktopPersistentDeviceIdentityStore()
}
