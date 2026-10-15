package com.pronavicola.camion.service

import com.pronavicola.camion.data.remote.ScreensApiService
import com.pronavicola.camion.domain.repository.AppConfigRepository
import com.pronavicola.camion.util.Log
import com.pronavicola.camion.util.PlatformInfoProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

class DevicePairingService(
    private val platformInfoProvider: PlatformInfoProvider,
    private val screensApiService: ScreensApiService,
    private val appConfigRepository: AppConfigRepository,
    private val scope: CoroutineScope
) {
    private val tag = "DevicePairingService"
    private var tokenRefreshJob: Job? = null

    companion object {
        const val TOKEN_REFRESH_ADVANCE_HOURS = 24L
        const val DEFAULT_SERVER_URL = "https://screens-api.pronavicola.com"
    }

    /**
     * Initializes device ID from existing config, hardware fingerprint, or generates UUID fallback.
     */
    suspend fun initializeDeviceId(): String {
        var deviceId = appConfigRepository.getDeviceId()

        if (deviceId.isNullOrBlank()) {
            val fingerprint = try {
                platformInfoProvider.getHardwareFingerprint().trim()
            } catch (e: Exception) {
                Log.w(tag, "Could not obtain hardware fingerprint: ${e.message}")
                ""
            }

            deviceId = if (fingerprint.isNotBlank()) fingerprint else UUID.randomUUID().toString()
            appConfigRepository.saveDeviceId(deviceId)
            Log.i(tag, "Initialized new device ID: $deviceId")
        } else {
            Log.d(tag, "Existing device ID loaded: $deviceId")
        }

        return deviceId
    }

    /**
     * Checks pairing status with backend Screens API and updates local configuration if ACTIVE.
     */
    suspend fun checkPairingStatus(pairingCode: String): Result<Boolean> {
        val deviceId = initializeDeviceId()
        val baseUrl = appConfigRepository.getServerBaseUrl() ?: DEFAULT_SERVER_URL

        val startTime = System.currentTimeMillis()
        return try {
            val result = screensApiService.checkPairingStatus(baseUrl, pairingCode, deviceId)
            val duration = System.currentTimeMillis() - startTime
            Log.i(tag, "API checkPairingStatus completed in ${duration}ms (status: ${result.isSuccess})")

            result.fold(
                onSuccess = { response ->
                    if (response.status == "ACTIVE") {
                        response.deviceToken?.let { token ->
                            appConfigRepository.saveDeviceToken(token)

                            val expiry = extractJwtExpiry(token)
                            if (expiry != null) {
                                appConfigRepository.saveTokenExpiry(expiry)
                                scheduleTokenRefresh(expiry)
                            }
                        }

                        response.furgon?.let { furgon ->
                            appConfigRepository.saveFurgonId(furgon.id)
                            appConfigRepository.saveFurgonPlate(furgon.plate)
                        }

                        response.mqttConfig?.let { mqtt ->
                            mqtt.brokerUrl?.let { appConfigRepository.saveBrokerUrl(it) }
                            mqtt.topicRoot?.let { appConfigRepository.saveTopicRoot(it) }
                        }

                        appConfigRepository.saveLicenseStatus("ACTIVE")
                        Log.i(tag, "Device paired successfully with furgon: ${response.furgon?.plate}")
                        Result.success(true)
                    } else {
                        Log.d(tag, "Pairing check returned status: ${response.status}")
                        Result.success(false)
                    }
                },
                onFailure = { error ->
                    Log.e(tag, "Pairing check failed: ${error.message}", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(tag, "Exception checking pairing status: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Extracts token expiration timestamp in milliseconds from JWT payload claims.
     */
    fun extractJwtExpiry(token: String): Long? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null

            var payloadPart = parts[1].replace('-', '+').replace('_', '/')
            while (payloadPart.length % 4 != 0) {
                payloadPart += "="
            }

            val decodedBytes = Base64.getDecoder().decode(payloadPart)
            val payloadJson = String(decodedBytes, StandardCharsets.UTF_8)
            val json = JSONObject(payloadJson)
            val expSeconds = json.optLong("exp", 0L)

            if (expSeconds > 0) expSeconds * 1000L else null
        } catch (e: Exception) {
            Log.w(tag, "Failed to parse JWT expiry from token: ${e.message}")
            null
        }
    }

    /**
     * Schedules automatic token refresh 24 hours prior to expiration timestamp.
     */
    fun scheduleTokenRefresh(expiryTimestamp: Long) {
        tokenRefreshJob?.cancel()

        val now = System.currentTimeMillis()
        val refreshTarget = expiryTimestamp - (TOKEN_REFRESH_ADVANCE_HOURS * 3600 * 1000L)
        val delayMs = (refreshTarget - now).coerceAtLeast(0L)

        tokenRefreshJob = scope.launch(Dispatchers.IO) {
            Log.i(tag, "Scheduling token refresh in ${delayMs / 1000 / 3600}h (${delayMs}ms)")
            delay(delayMs)
            refreshToken()
        }
    }

    suspend fun refreshToken(attempt: Int = 0) {
        val deviceToken = appConfigRepository.getDeviceToken()
        val baseUrl = appConfigRepository.getServerBaseUrl() ?: DEFAULT_SERVER_URL

        if (deviceToken.isNullOrBlank()) {
            Log.w(tag, "No device token present to refresh")
            return
        }

        try {
            val result = screensApiService.validateScreen(baseUrl, deviceToken, null)
            result.fold(
                onSuccess = { response ->
                    if (response.isValid) {
                        Log.i(tag, "Device token validated successfully")
                    } else {
                        Log.w(tag, "Token validation returned false. Marking device as unpaired.")
                        markDeviceUnpaired()
                    }
                },
                onFailure = { error ->
                    if (error is ScreensApiService.ApiException && error.code == 401) {
                        Log.w(tag, "Token validation failed with HTTP 401. Marking device as unpaired.")
                        markDeviceUnpaired()
                    } else {
                        Log.w(tag, "Token validation failed (${error.message}). Retrying (attempt ${attempt + 1})...")
                        retryTokenRefresh(attempt + 1)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(tag, "Exception during token refresh: ${e.message}", e)
            retryTokenRefresh(attempt + 1)
        }
    }

    private fun retryTokenRefresh(attempt: Int) {
        if (attempt >= 5) {
            Log.e(tag, "Token refresh reached maximum retry limit (5 attempts). Aborting.")
            return
        }

        val delayMs = (2000L * (1 shl attempt.coerceAtMost(10))).coerceAtMost(300_000L)
        scope.launch(Dispatchers.IO) {
            delay(delayMs)
            refreshToken(attempt)
        }
    }

    suspend fun markDeviceUnpaired() {
        appConfigRepository.remove("deviceToken")
        appConfigRepository.remove("device_token")
        appConfigRepository.remove("device_token_expiry")
        appConfigRepository.remove("furgon_id")
        appConfigRepository.remove("furgon_plate")
        appConfigRepository.saveLicenseStatus("UNPAIRED")
        Log.i(tag, "Device marked as unpaired locally")
    }
}
