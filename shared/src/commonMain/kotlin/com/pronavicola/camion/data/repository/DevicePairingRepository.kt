package com.pronavicola.camion.data.repository

import com.pronavicola.camion.data.remote.FurgonInfo
import com.pronavicola.camion.data.remote.ScreensApiService
import com.pronavicola.camion.data.remote.ValidateScreenResponse
import com.pronavicola.camion.domain.repository.AppConfigRepository
import com.pronavicola.camion.domain.repository.DevicePairingRepository
import com.pronavicola.camion.util.Log
import com.pronavicola.camion.util.PlatformInfoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DevicePairingRepositoryImpl(
    private val platformInfoProvider: PlatformInfoProvider,
    private val apiService: ScreensApiService,
    private val configRepository: AppConfigRepository,
) : DevicePairingRepository {
    private val tag = "DevicePairingRepository"

    override fun getHardwareFingerprint(): String = platformInfoProvider.getHardwareFingerprint()

    override suspend fun requestPairingCode(serverUrl: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        val fingerprint = platformInfoProvider.getHardwareFingerprint()
        val brand = platformInfoProvider.getDeviceBrand()
        val model = platformInfoProvider.getDeviceModel()
        val osVersion = platformInfoProvider.getOsVersion()
        val appVersion = platformInfoProvider.getAppVersion()

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

    override suspend fun checkPairingStatus(serverUrl: String, pairingCode: String, deviceId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = serverUrl.trim().removeSuffix("/")
        val cleanCode = pairingCode.trim()
        val cleanDeviceId = deviceId.trim()

        // 1. If pairingCode is provided, check pairing code status on the server
        if (cleanCode.isNotBlank()) {
            val result = apiService.checkPairingStatus(cleanUrl, cleanCode, cleanDeviceId)
            if (result.isSuccess) {
                val resp = result.getOrThrow()
                configRepository.saveLicenseStatus(resp.status)

                if (resp.status == "ACTIVE") {
                    resp.deviceToken?.let { configRepository.saveDeviceToken(it) }
                    resp.licenseKey?.let { configRepository.saveLicenseKey(it) }
                    if (resp.deviceId.isNotBlank()) {
                        configRepository.saveDeviceId(resp.deviceId)
                    }
                    resp.furgon?.plate?.takeIf { it != resp.deviceId }?.let { configRepository.saveFurgonPlate(it) }
                    resp.mqttConfig?.brokerUrl?.let { configRepository.saveBrokerUrl(it) }
                    resp.mqttConfig?.topicRoot?.let { configRepository.saveTopicRoot(it) }
                    Log.i(tag, "Screen is ACTIVE and paired successfully (deviceId=${resp.deviceId}, furgon=${resp.furgon?.plate})")
                    return@withContext Result.success(true)
                } else {
                    return@withContext Result.success(false)
                }
            } else {
                val ex = result.exceptionOrNull()
                if (ex !is ScreensApiService.ApiException) {
                    return@withContext Result.failure(ex ?: Exception("Error de conexión con la API"))
                }
            }
        }

        // 2. Query validate endpoint with deviceId to check if already registered
        val devToken = configRepository.getDeviceToken()
        val validateResult = apiService.validateScreen(cleanUrl, devToken, cleanDeviceId)
        if (validateResult.isSuccess) {
            val vResp = validateResult.getOrThrow()
            if (vResp.isValid && vResp.status == "ACTIVE") {
                configRepository.saveLicenseStatus("ACTIVE")
                if (cleanDeviceId.isNotBlank()) {
                    configRepository.saveDeviceId(cleanDeviceId)
                }
                vResp.deviceToken?.let { configRepository.saveDeviceToken(it) }
                vResp.licenseKey?.let { configRepository.saveLicenseKey(it) }
                vResp.mqttConfig?.brokerUrl?.let { configRepository.saveBrokerUrl(it) }
                vResp.mqttConfig?.topicRoot?.let { configRepository.saveTopicRoot(it) }

                val plate = vResp.furgonPlate ?: vResp.furgon?.plate
                if (!plate.isNullOrBlank() && plate != cleanDeviceId) {
                    configRepository.saveFurgonPlate(plate)
                }
                Log.i(tag, "Prior registration validated (deviceId=$cleanDeviceId, plate=$plate)")
                return@withContext Result.success(true)
            } else if (vResp.status == "PENDING_PAIRING" || vResp.status == "PENDING") {
                configRepository.saveLicenseStatus("PENDING_PAIRING")
                return@withContext Result.success(false)
            } else {
                return@withContext Result.failure(Exception("No se encontró registro previo, solicita PIN o ingresa clave"))
            }
        } else {
            val vEx = validateResult.exceptionOrNull()
            if (vEx !is ScreensApiService.ApiException) {
                return@withContext Result.failure(vEx ?: Exception("Error de conexión con la API"))
            } else {
                return@withContext Result.failure(Exception("No se encontró registro previo, solicita PIN o ingresa clave"))
            }
        }
    }

    override suspend fun activateLicenseKey(serverUrl: String, licenseKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val fingerprint = platformInfoProvider.getHardwareFingerprint()
        val brand = platformInfoProvider.getDeviceBrand()
        val model = platformInfoProvider.getDeviceModel()
        val osVersion = platformInfoProvider.getOsVersion()
        val appVersion = platformInfoProvider.getAppVersion()

        val result = apiService.activateLicenseKey(serverUrl, licenseKey, fingerprint, brand, model, osVersion, appVersion)
        if (result.isSuccess) {
            val resp = result.getOrThrow()
            configRepository.saveServerBaseUrl(serverUrl)
            configRepository.saveLicenseKey(resp.licenseKey ?: licenseKey)
            configRepository.saveLicenseStatus(resp.status)

            if (resp.status == "ACTIVE") {
                resp.deviceToken?.let { configRepository.saveDeviceToken(it) }
                resp.deviceId.let { configRepository.saveDeviceId(it) }
                resp.furgon?.plate?.takeIf { it != resp.deviceId }?.let { configRepository.saveFurgonPlate(it) }
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

    override suspend fun validateScreen(serverUrl: String, deviceToken: String?, deviceId: String?): Result<ValidateScreenResponse> = withContext(Dispatchers.IO) {
        val result = apiService.validateScreen(serverUrl, deviceToken, deviceId)
        if (result.isSuccess) {
            val resp = result.getOrThrow()
            configRepository.saveLicenseStatus(resp.status)
            resp.deviceToken?.let { configRepository.saveDeviceToken(it) }
            resp.licenseKey?.let { configRepository.saveLicenseKey(it) }
            resp.furgonPlate?.takeIf { it != deviceId }?.let { configRepository.saveFurgonPlate(it) }
            Result.success(resp)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Error de validación"))
        }
    }

    override suspend fun getAvailableFurgones(serverUrl: String, deviceToken: String?, deviceId: String?): Result<List<FurgonInfo>> = withContext(Dispatchers.IO) {
        val token = deviceToken ?: configRepository.getDeviceToken()
        val devId = deviceId ?: configRepository.getDeviceId()
        val url = serverUrl.ifBlank { configRepository.getServerBaseUrl() ?: "" }
        val result = apiService.getAvailableFurgones(url, token, devId)
        if (result.isSuccess) {
            val resp = result.getOrThrow()
            resp.assignedFurgon?.plate?.takeIf { it != devId }?.let { configRepository.saveFurgonPlate(it) }
            Result.success(resp.availableFurgones)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Error obteniendo furgones"))
        }
    }

    override suspend fun updateAssociatedFurgon(serverUrl: String, deviceToken: String?, furgonId: String, plate: String?): Result<String> = withContext(Dispatchers.IO) {
        val token = deviceToken ?: configRepository.getDeviceToken()
        val devId = configRepository.getDeviceId()
        val url = serverUrl.ifBlank { configRepository.getServerBaseUrl() ?: "" }
        val result = apiService.updateAssociatedFurgon(url, token, furgonId, plate, devId)
        if (result.isSuccess) {
            val furgon = result.getOrThrow()
            if (furgon.plate.isNotBlank() && furgon.plate != devId) {
                configRepository.saveFurgonPlate(furgon.plate)
            }
            Result.success(furgon.plate)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Error actualizando furgón"))
        }
    }

    override suspend fun unpairDevice(serverUrl: String, deviceToken: String?): Result<Unit> = withContext(Dispatchers.IO) {
        val token = deviceToken ?: configRepository.getDeviceToken()
        val devId = configRepository.getDeviceId()
        val url = serverUrl.ifBlank { configRepository.getServerBaseUrl() ?: "" }
        if (url.isNotBlank()) {
            try {
                apiService.unpairDevice(url, token, devId)
            } catch (e: Exception) {
                Log.w(tag, "Remote unpair error: ${e.message}, proceeding with local unpair")
            }
        }
        configRepository.saveLicenseStatus("UNPAIRED")
        configRepository.saveDeviceToken("")
        configRepository.savePairingCode("")
        configRepository.saveLicenseKey("")
        Log.i(tag, "Device unpaired successfully (local credentials cleared)")
        Result.success(Unit)
    }
}
