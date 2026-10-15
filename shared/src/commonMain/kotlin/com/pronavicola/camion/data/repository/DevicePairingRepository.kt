package com.pronavicola.camion.data.repository

import com.pronavicola.camion.data.local.dao.VehiculoDao
import com.pronavicola.camion.data.local.entity.VehiculoEntity
import com.pronavicola.camion.data.remote.FurgonInfo
import com.pronavicola.camion.data.remote.ScreensApiService
import com.pronavicola.camion.data.remote.ValidateScreenResponse
import com.pronavicola.camion.domain.repository.AppConfigRepository
import com.pronavicola.camion.domain.repository.DevicePairingRepository
import com.pronavicola.camion.util.Log
import com.pronavicola.camion.util.PlatformInfoProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.pronavicola.camion.util.PersistentDeviceIdentityStore
import com.pronavicola.camion.util.StoredDeviceIdentity

class DevicePairingRepositoryImpl(
    private val platformInfoProvider: PlatformInfoProvider,
    private val apiService: ScreensApiService,
    private val configRepository: AppConfigRepository,
    private val vehiculoDao: VehiculoDao? = null,
    private val persistentIdentityStore: PersistentDeviceIdentityStore? = null,
) : DevicePairingRepository {
    private val tag = "DevicePairingRepository"

    private fun normalizeBrokerUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("tcp://", ignoreCase = true) -> "mqtt://" + trimmed.removePrefix("tcp://")
            trimmed.startsWith("ssl://", ignoreCase = true) -> "mqtts://" + trimmed.removePrefix("ssl://")
            !trimmed.startsWith("mqtt://", ignoreCase = true) && !trimmed.startsWith("mqtts://", ignoreCase = true) -> "mqtt://$trimmed"
            else -> trimmed
        }
    }

    private suspend fun syncFurgonPlate(plate: String) {
        val cleanPlate = plate.trim().uppercase()
        if (cleanPlate.isBlank()) return
        configRepository.saveFurgonPlate(cleanPlate)
        vehiculoDao?.let { dao ->
            try {
                val existing = dao.getByPlaca(cleanPlate)
                dao.clearPredeterminado()
                if (existing != null) {
                    dao.setPredeterminado(existing.idVehiculo)
                } else {
                    dao.insert(VehiculoEntity(idVehiculo = 0, placa = cleanPlate, predeterminado = 1))
                }
            } catch (e: Exception) {
                Log.w(tag, "Error syncing vehicle plate $cleanPlate: ${e.message}")
            }
        }
    }

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
            persistentIdentityStore?.saveIdentity(
                StoredDeviceIdentity(
                    deviceId = resp.deviceId,
                    serverBaseUrl = serverUrl,
                )
            )
            Log.i(tag, "Pairing code requested successfully: ${resp.pairingCode} for deviceId: ${resp.deviceId}")
            Result.success(Pair(resp.deviceId, resp.pairingCode))
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Error solicitando PIN"))
        }
    }

    override suspend fun checkPairingStatus(serverUrl: String, pairingCode: String, deviceId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = serverUrl.trim().removeSuffix("/")
        val cleanCode = pairingCode.trim()
        var targetDeviceId = deviceId.trim()

        // Si targetDeviceId está vacío o es un valor por defecto, intentar recuperarlo del almacén persistente
        if (targetDeviceId.isBlank() || targetDeviceId == "ABC123") {
            persistentIdentityStore?.loadIdentity()?.let { stored ->
                if (stored.deviceId.isNotBlank()) {
                    targetDeviceId = stored.deviceId
                    configRepository.saveDeviceId(stored.deviceId)
                    stored.licenseKey?.let { configRepository.saveLicenseKey(it) }
                    stored.furgonPlate?.let { configRepository.saveFurgonPlate(it) }
                }
            }
        }

        // 1. If pairingCode is provided, check pairing code status on the server
        if (cleanCode.isNotBlank()) {
            val result = apiService.checkPairingStatus(cleanUrl, cleanCode, targetDeviceId)
            if (result.isSuccess) {
                val resp = result.getOrThrow()
                configRepository.saveLicenseStatus(resp.status)

                if (resp.status == "ACTIVE") {
                    resp.deviceToken?.let { configRepository.saveDeviceToken(it) }
                    resp.licenseKey?.let { configRepository.saveLicenseKey(it) }
                    val finalDevId = if (resp.deviceId.isNotBlank()) resp.deviceId else targetDeviceId
                    if (finalDevId.isNotBlank()) {
                        configRepository.saveDeviceId(finalDevId)
                    }
                    val plate = resp.furgon?.plate?.takeIf { it != finalDevId }
                    plate?.let { syncFurgonPlate(it) }
                    resp.mqttConfig?.brokerUrl?.let { configRepository.saveBrokerUrl(normalizeBrokerUrl(it) ?: it) }
                    resp.mqttConfig?.topicRoot?.let { configRepository.saveTopicRoot(it) }
                    resp.mqttConfig?.username?.let { configRepository.saveBrokerUsername(it) }
                    resp.mqttConfig?.password?.let { configRepository.saveBrokerPassword(it) }

                    persistentIdentityStore?.saveIdentity(
                        StoredDeviceIdentity(
                            deviceId = finalDevId,
                            licenseKey = resp.licenseKey,
                            serverBaseUrl = cleanUrl,
                            furgonPlate = plate,
                            deviceToken = resp.deviceToken,
                        )
                    )
                    Log.i(tag, "Screen is ACTIVE and paired successfully (deviceId=$finalDevId, furgon=$plate)")
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
        val validateResult = apiService.validateScreen(cleanUrl, devToken, targetDeviceId)
        if (validateResult.isSuccess) {
            val vResp = validateResult.getOrThrow()
            if (vResp.isValid && vResp.status == "ACTIVE") {
                configRepository.saveLicenseStatus("ACTIVE")
                val finalDevId = vResp.deviceId ?: targetDeviceId
                if (finalDevId.isNotBlank()) {
                    configRepository.saveDeviceId(finalDevId)
                }
                vResp.deviceToken?.let { configRepository.saveDeviceToken(it) }
                vResp.licenseKey?.let { configRepository.saveLicenseKey(it) }
                vResp.mqttConfig?.brokerUrl?.let { configRepository.saveBrokerUrl(normalizeBrokerUrl(it) ?: it) }
                vResp.mqttConfig?.topicRoot?.let { configRepository.saveTopicRoot(it) }
                vResp.mqttConfig?.username?.let { configRepository.saveBrokerUsername(it) }
                vResp.mqttConfig?.password?.let { configRepository.saveBrokerPassword(it) }

                val plate = vResp.furgonPlate ?: vResp.furgon?.plate
                if (!plate.isNullOrBlank() && plate != finalDevId) {
                    syncFurgonPlate(plate)
                }
                persistentIdentityStore?.saveIdentity(
                    StoredDeviceIdentity(
                        deviceId = finalDevId,
                        licenseKey = vResp.licenseKey ?: configRepository.getLicenseKey(),
                        serverBaseUrl = cleanUrl,
                        furgonPlate = plate,
                        deviceToken = vResp.deviceToken ?: devToken,
                    )
                )
                Log.i(tag, "Prior registration validated (deviceId=$finalDevId, plate=$plate)")
                return@withContext Result.success(true)
            } else {
                // Si la validación directa no está activa, intentar reactivación con clave de licencia si existe
                val savedLicenseKey = configRepository.getLicenseKey()
                    ?: persistentIdentityStore?.loadIdentity()?.licenseKey
                if (!savedLicenseKey.isNullOrBlank()) {
                    Log.i(tag, "Attempting auto-activation with stored license key: $savedLicenseKey")
                    val activateResult = activateLicenseKey(cleanUrl, savedLicenseKey)
                    if (activateResult.isSuccess && activateResult.getOrThrow()) {
                        return@withContext Result.success(true)
                    }
                }

                if (vResp.status == "PENDING_PAIRING" || vResp.status == "PENDING") {
                    configRepository.saveLicenseStatus("PENDING_PAIRING")
                    return@withContext Result.success(false)
                } else {
                    configRepository.saveLicenseStatus(vResp.status)
                    return@withContext Result.failure(Exception("No se encontró registro previo activo para el dispositivo en FURGN BI. Solicite un PIN o active con su clave de registro."))
                }
            }
        } else {
            // Si la llamada directa falló (por ejemplo, 401 porque no hay token ni deviceId registrado),
            // verificar si podemos auto-activar con clave de licencia
            val savedLicenseKey = configRepository.getLicenseKey()
                ?: persistentIdentityStore?.loadIdentity()?.licenseKey
            if (!savedLicenseKey.isNullOrBlank()) {
                val activateResult = activateLicenseKey(cleanUrl, savedLicenseKey)
                if (activateResult.isSuccess && activateResult.getOrThrow()) {
                    return@withContext Result.success(true)
                }
            }
            val ex = validateResult.exceptionOrNull()
            return@withContext Result.failure(ex ?: Exception("Error validando registro previo"))
        }
    }

    override suspend fun activateLicenseKey(
        serverUrl: String,
        licenseKey: String,
    ): Result<Boolean> = withContext(Dispatchers.IO) {
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
                val plate = resp.furgon?.plate?.takeIf { it != resp.deviceId }
                plate?.let { syncFurgonPlate(it) }
                resp.mqttConfig?.brokerUrl?.let { configRepository.saveBrokerUrl(normalizeBrokerUrl(it) ?: it) }
                resp.mqttConfig?.topicRoot?.let { configRepository.saveTopicRoot(it) }
                resp.mqttConfig?.username?.let { configRepository.saveBrokerUsername(it) }
                resp.mqttConfig?.password?.let { configRepository.saveBrokerPassword(it) }

                persistentIdentityStore?.saveIdentity(
                    StoredDeviceIdentity(
                        deviceId = resp.deviceId,
                        licenseKey = resp.licenseKey ?: licenseKey,
                        serverBaseUrl = serverUrl,
                        furgonPlate = plate,
                        deviceToken = resp.deviceToken,
                    )
                )
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
            resp.furgonPlate?.takeIf { it != deviceId }?.let { syncFurgonPlate(it) }
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
            resp.assignedFurgon?.plate?.takeIf { it != devId }?.let { syncFurgonPlate(it) }
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
                syncFurgonPlate(furgon.plate)
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
        persistentIdentityStore?.clearIdentity()
        Log.i(tag, "Device unpaired successfully (local credentials cleared)")
        Result.success(Unit)
    }
}
