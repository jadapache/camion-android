package com.pronavicola.camion.util

data class StoredDeviceIdentity(
    val deviceId: String,
    val licenseKey: String? = null,
    val serverBaseUrl: String? = null,
    val furgonPlate: String? = null,
    val deviceToken: String? = null,
)

interface PersistentDeviceIdentityStore {
    fun saveIdentity(identity: StoredDeviceIdentity)
    fun loadIdentity(): StoredDeviceIdentity?
    fun clearIdentity()
}

expect fun getPersistentDeviceIdentityStore(platformContext: Any? = null): PersistentDeviceIdentityStore
