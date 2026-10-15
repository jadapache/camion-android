package com.pronavicola.camion.domain.repository

interface AppConfigRepository {
    suspend fun getDeviceId(): String?
    suspend fun saveDeviceId(id: String)

    suspend fun getFurgonId(): String?
    suspend fun saveFurgonId(id: String)

    suspend fun getFurgonPlate(): String?
    suspend fun saveFurgonPlate(plate: String)

    suspend fun getBrokerUrl(): String?
    suspend fun saveBrokerUrl(url: String)

    suspend fun getTopicRoot(): String?
    suspend fun saveTopicRoot(root: String)

    suspend fun getBrokerUsername(): String?
    suspend fun saveBrokerUsername(username: String)

    suspend fun getBrokerPassword(): String?
    suspend fun saveBrokerPassword(password: String)

    suspend fun getModbusHost(): String?
    suspend fun saveModbusHost(host: String)

    suspend fun getModbusPort(): String?
    suspend fun saveModbusPort(port: String)

    suspend fun getPublishInterval(): String?
    suspend fun savePublishInterval(interval: String)

    suspend fun getServerBaseUrl(): String?
    suspend fun saveServerBaseUrl(url: String)

    suspend fun getDeviceToken(): String?
    suspend fun saveDeviceToken(token: String)

    suspend fun getTokenExpiry(): Long?
    suspend fun saveTokenExpiry(expiry: Long)

    suspend fun getPairingCode(): String?
    suspend fun savePairingCode(code: String)

    suspend fun getLicenseKey(): String?
    suspend fun saveLicenseKey(key: String)

    suspend fun getLicenseStatus(): String?
    suspend fun saveLicenseStatus(status: String)

    fun observeLicenseStatus(): kotlinx.coroutines.flow.Flow<String?> = kotlinx.coroutines.flow.flowOf(null)
    fun observeFurgonPlate(): kotlinx.coroutines.flow.Flow<String?> = kotlinx.coroutines.flow.flowOf(null)

    suspend fun getModbusSimulationEnabled(): Boolean
    suspend fun saveModbusSimulationEnabled(enabled: Boolean)

    // Dynamic key-value access helpers
    suspend fun getString(key: String): String?
    suspend fun putString(key: String, value: String)
    suspend fun getLong(key: String): Long?
    suspend fun putLong(key: String, value: Long)
    suspend fun remove(key: String)
}
