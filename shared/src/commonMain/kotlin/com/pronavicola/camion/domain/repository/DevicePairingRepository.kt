package com.pronavicola.camion.domain.repository

import com.pronavicola.camion.data.remote.FurgonInfo
import com.pronavicola.camion.data.remote.ValidateScreenResponse

interface DevicePairingRepository {
    fun getHardwareFingerprint(): String
    suspend fun requestPairingCode(serverUrl: String): Result<Pair<String, String>>
    suspend fun checkPairingStatus(serverUrl: String, pairingCode: String, deviceId: String): Result<Boolean>
    suspend fun activateLicenseKey(serverUrl: String, licenseKey: String): Result<Boolean>
    suspend fun validateScreen(serverUrl: String, deviceToken: String?, deviceId: String?): Result<ValidateScreenResponse>
    suspend fun getAvailableFurgones(serverUrl: String, deviceToken: String?): Result<List<FurgonInfo>>
    suspend fun updateAssociatedFurgon(serverUrl: String, deviceToken: String?, furgonId: String): Result<String>
    suspend fun unpairDevice(serverUrl: String, deviceToken: String?): Result<Unit>
}
