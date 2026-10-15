package com.pronavicola.camion.util

import android.content.Context
import android.os.Environment
import org.json.JSONObject
import java.io.File

class AndroidPersistentDeviceIdentityStore(
    private val context: Context,
) : PersistentDeviceIdentityStore {
    private val tag = "DeviceIdentityStore"
    private val fileName = ".camion_device_identity.json"

    private fun getCandidateFiles(): List<File> {
        val list = mutableListOf<File>()
        try {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (docsDir != null) {
                list.add(File(docsDir, fileName))
            }
        } catch (_: Exception) {}

        try {
            val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (dlDir != null) {
                list.add(File(dlDir, fileName))
            }
        } catch (_: Exception) {}

        try {
            list.add(File(context.filesDir, fileName))
        } catch (_: Exception) {}

        return list
    }

    override fun saveIdentity(identity: StoredDeviceIdentity) {
        if (identity.deviceId.isBlank()) return
        val json = JSONObject().apply {
            put("device_id", identity.deviceId)
            put("license_key", identity.licenseKey ?: "")
            put("server_base_url", identity.serverBaseUrl ?: "")
            put("furgon_plate", identity.furgonPlate ?: "")
            put("device_token", identity.deviceToken ?: "")
            put("updated_at", System.currentTimeMillis())
        }.toString()

        for (file in getCandidateFiles()) {
            try {
                file.parentFile?.let { if (!it.exists()) it.mkdirs() }
                file.writeText(json)
            } catch (e: Exception) {
                Log.w(tag, "Could not save identity to ${file.absolutePath}: ${e.message}")
            }
        }
    }

    override fun loadIdentity(): StoredDeviceIdentity? {
        for (file in getCandidateFiles()) {
            try {
                if (file.exists() && file.canRead()) {
                    val text = file.readText().trim()
                    if (text.isNotBlank()) {
                        val obj = JSONObject(text)
                        val devId = obj.optString("device_id", "").trim()
                        if (devId.isNotBlank()) {
                            return StoredDeviceIdentity(
                                deviceId = devId,
                                licenseKey = obj.optString("license_key", "").takeIf { it.isNotBlank() },
                                serverBaseUrl = obj.optString("server_base_url", "").takeIf { it.isNotBlank() },
                                furgonPlate = obj.optString("furgon_plate", "").takeIf { it.isNotBlank() },
                                deviceToken = obj.optString("device_token", "").takeIf { it.isNotBlank() },
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Could not read identity from ${file.absolutePath}: ${e.message}")
            }
        }
        return null
    }

    override fun clearIdentity() {
        for (file in getCandidateFiles()) {
            try {
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
    }
}

actual fun getPersistentDeviceIdentityStore(platformContext: Any?): PersistentDeviceIdentityStore {
    val context = platformContext as? Context ?: error("Context required for Android PersistentDeviceIdentityStore")
    return AndroidPersistentDeviceIdentityStore(context)
}
