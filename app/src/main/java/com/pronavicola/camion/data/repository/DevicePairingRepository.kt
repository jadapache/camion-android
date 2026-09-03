package com.pronavicola.camion.data.repository

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.pronavicola.camion.data.remote.FurgonInfo
import com.pronavicola.camion.data.remote.ScreensApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DevicePairingRepository(
    private val context: Context,
    private val apiService: ScreensApiService,
    private val configRepository: AppConfigRepository,
) {
    private val tag = "DevicePairingRepository"

    fun getHardwareFingerprint(): String {
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

    suspend fun requestPairingCode(serverUrl: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        val fingerprint = getHardwareFingerprint()
        val brand = Build.MANUFACTURER
        val model = Build.MODEL
        val osVersion = "Android ${Build.VERSION.RELEASE}"
        val appVersion = "1.0.0"

        val result = apiService.requestPairingCode(serverUrl, fingerprint, brand, model, osVersion, appVersion)
        if (result.isSuccess) {
            val resp = result.getOrThrow()
            configRepository.saveServerBaseUrl(serverUrl)
            configRepository.saveDeviceId(resp.deviceId)
            configRepository.savePairingCode(resp.pairingCode)
            configRepository.saveLicenseStatus("PENDING_PAIRING")
            Log.i(tag, "Pairing code requested successfully: ${resp.pairingCode} for deviceId: ${resp.deviceId}")
            Result.success(Pair(resp.deviceId, resp.pairingCode))
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Error solicitando PIN"))
        }
    }

    suspend fun checkPairingStatus(serverUrl: String, pairingCode: String, deviceId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val result = apiService.checkPairingStatus(serverUrl, pairingCode, deviceId)
        if (result.isSuccess) {
            val resp = result.getOrThrow()
            configRepository.saveLicenseStatus(resp.status)

            if (resp.status == "ACTIVE") {
                resp.deviceToken?.let { configRepository.saveDeviceToken(it) }
                resp.furgon?.plate?.let { configRepository.saveDeviceId(it) } // Or sync plate
                resp.mqttConfig?.brokerUrl?.let { configRepository.saveBrokerUrl(it) }
                resp.mqttConfig?.topicRoot?.let { configRepository.saveTopicRoot(it) }
                Log.i(tag, "Screen is ACTIVE and paired successfully")
                Result.success(true)
            } else {
                Result.success(false)
            }
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Error verificando estado"))
        }
    }

    suspend fun activateLicenseKey(serverUrl: String, licenseKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val fingerprint = getHardwareFingerprint()
        val brand = Build.MANUFACTURER
        val model = Build.MODEL
        val osVersion = "Android ${Build.VERSION.RELEASE}"
        val appVersion = "1.0.0"

        val result = apiService.activateLicenseKey(serverUrl, licenseKey, fingerprint, brand, model, osVersion, appVersion)
        if (result.isSuccess) {
            val resp = result.getOrThrow()
            configRepository.saveServerBaseUrl(serverUrl)
            configRepository.saveLicenseKey(licenseKey)
            configRepository.saveLicenseStatus(resp.status)

            if (resp.status == "ACTIVE") {
                resp.deviceToken?.let { configRepository.saveDeviceToken(it) }
                resp.deviceId.let { configRepository.saveDeviceId(it) }
                resp.mqttConfig?.brokerUrl?.let { configRepository.saveBrokerUrl(it) }
                resp.mqttConfig?.topicRoot?.let { configRepository.saveTopicRoot(it) }
                Log.i(tag, "Activated successfully via license key")
                Result.success(true)
            } else {
                Result.success(false)
            }
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Error activando clave de licencia"))
        }
    }

    suspend fun validateScreen(serverUrl: String, deviceToken: String?, deviceId: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        val result = apiService.validateScreen(serverUrl, deviceToken, deviceId)
        if (result.isSuccess) {
            val resp = result.getOrThrow()
            configRepository.saveLicenseStatus(resp.status)
            Result.success(resp.isValid)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Error de validación"))
        }
    }

    suspend fun getAvailableFurgones(serverUrl: String, deviceToken: String?): Result<List<FurgonInfo>> = withContext(Dispatchers.IO) {
        val result = apiService.getAvailableFurgones(serverUrl, deviceToken)
        if (result.isSuccess) {
            Result.success(result.getOrThrow().availableFurgones)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Error obteniendo furgones"))
        }
    }

    suspend fun updateAssociatedFurgon(serverUrl: String, deviceToken: String?, furgonId: String): Result<String> = withContext(Dispatchers.IO) {
        val result = apiService.updateAssociatedFurgon(serverUrl, deviceToken, furgonId)
        if (result.isSuccess) {
            val furgon = result.getOrThrow()
            configRepository.saveDeviceId(furgon.plate)
            Result.success(furgon.plate)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Error actualizando furgón"))
        }
    }
}
