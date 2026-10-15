package com.pronavicola.camion.data.repository

import com.pronavicola.camion.data.local.dao.AppConfigDao
import com.pronavicola.camion.data.local.entity.AppConfigEntity
import com.pronavicola.camion.domain.repository.AppConfigRepository
import kotlinx.coroutines.flow.Flow

class AppConfigRepositoryImpl(
    private val appConfigDao: AppConfigDao,
) : AppConfigRepository {
    companion object {
        const val KEY_DEVICE_ID = "deviceId"
        const val KEY_DEVICE_ID_SNAKE = "device_id"
        const val KEY_FURGON_ID = "furgon_id"
        const val KEY_FURGON_PLATE = "furgonPlate"
        const val KEY_FURGON_PLATE_SNAKE = "furgon_plate"
        const val KEY_BROKER_URL = "brokerUrl"
        const val KEY_BROKER_URL_SNAKE = "broker_url"
        const val KEY_TOPIC_ROOT = "topicRoot"
        const val KEY_TOPIC_ROOT_SNAKE = "topic_root"
        const val KEY_BROKER_USERNAME = "brokerUsername"
        const val KEY_BROKER_PASSWORD = "brokerPassword"
        const val KEY_MODBUS_HOST = "modbusHost"
        const val KEY_MODBUS_PORT = "modbusPort"
        const val KEY_PUBLISH_INTERVAL = "publishIntervalS"
        const val KEY_SERVER_BASE_URL = "serverBaseUrl"
        const val KEY_SERVER_URL_SNAKE = "server_url"
        const val KEY_DEVICE_TOKEN = "deviceToken"
        const val KEY_DEVICE_TOKEN_SNAKE = "device_token"
        const val KEY_TOKEN_EXPIRY = "device_token_expiry"
        const val KEY_PAIRING_CODE = "pairingCode"
        const val KEY_LICENSE_KEY = "licenseKey"
        const val KEY_LICENSE_STATUS = "licenseStatus"
        const val KEY_MODBUS_SIMULATION_ENABLED = "modbusSimulationEnabled"
    }

    override suspend fun getDeviceId(): String? =
        appConfigDao.getValor(KEY_DEVICE_ID_SNAKE) ?: appConfigDao.getValor(KEY_DEVICE_ID)

    override suspend fun saveDeviceId(id: String) {
        appConfigDao.save(AppConfigEntity(KEY_DEVICE_ID, id))
        appConfigDao.save(AppConfigEntity(KEY_DEVICE_ID_SNAKE, id))
        appConfigDao.save(AppConfigEntity(clave = id, valor = "DEVICE_ID_ANCHOR"))
    }

    override suspend fun getFurgonId(): String? = appConfigDao.getValor(KEY_FURGON_ID)
    override suspend fun saveFurgonId(id: String) = appConfigDao.save(AppConfigEntity(KEY_FURGON_ID, id))

    override suspend fun getFurgonPlate(): String? =
        appConfigDao.getValor(KEY_FURGON_PLATE_SNAKE) ?: appConfigDao.getValor(KEY_FURGON_PLATE)

    override suspend fun saveFurgonPlate(plate: String) {
        appConfigDao.save(AppConfigEntity(KEY_FURGON_PLATE, plate))
        appConfigDao.save(AppConfigEntity(KEY_FURGON_PLATE_SNAKE, plate))
    }

    override suspend fun getBrokerUrl(): String? =
        appConfigDao.getValor(KEY_BROKER_URL_SNAKE) ?: appConfigDao.getValor(KEY_BROKER_URL)

    override suspend fun saveBrokerUrl(url: String) {
        appConfigDao.save(AppConfigEntity(KEY_BROKER_URL, url))
        appConfigDao.save(AppConfigEntity(KEY_BROKER_URL_SNAKE, url))
    }

    override suspend fun getTopicRoot(): String? =
        appConfigDao.getValor(KEY_TOPIC_ROOT_SNAKE) ?: appConfigDao.getValor(KEY_TOPIC_ROOT)

    override suspend fun saveTopicRoot(root: String) {
        appConfigDao.save(AppConfigEntity(KEY_TOPIC_ROOT, root))
        appConfigDao.save(AppConfigEntity(KEY_TOPIC_ROOT_SNAKE, root))
    }

    override suspend fun getBrokerUsername(): String? = appConfigDao.getValor(KEY_BROKER_USERNAME)
    override suspend fun saveBrokerUsername(username: String) = appConfigDao.save(AppConfigEntity(KEY_BROKER_USERNAME, username))

    override suspend fun getBrokerPassword(): String? = appConfigDao.getValor(KEY_BROKER_PASSWORD)
    override suspend fun saveBrokerPassword(password: String) = appConfigDao.save(AppConfigEntity(KEY_BROKER_PASSWORD, password))

    override suspend fun getModbusHost(): String? = appConfigDao.getValor(KEY_MODBUS_HOST)
    override suspend fun saveModbusHost(host: String) = appConfigDao.save(AppConfigEntity(KEY_MODBUS_HOST, host))

    override suspend fun getModbusPort(): String? = appConfigDao.getValor(KEY_MODBUS_PORT)
    override suspend fun saveModbusPort(port: String) = appConfigDao.save(AppConfigEntity(KEY_MODBUS_PORT, port))

    override suspend fun getPublishInterval(): String? = appConfigDao.getValor(KEY_PUBLISH_INTERVAL)
    override suspend fun savePublishInterval(interval: String) = appConfigDao.save(AppConfigEntity(KEY_PUBLISH_INTERVAL, interval))

    override suspend fun getServerBaseUrl(): String? =
        appConfigDao.getValor(KEY_SERVER_URL_SNAKE) ?: appConfigDao.getValor(KEY_SERVER_BASE_URL)

    override suspend fun saveServerBaseUrl(url: String) {
        appConfigDao.save(AppConfigEntity(KEY_SERVER_BASE_URL, url))
        appConfigDao.save(AppConfigEntity(KEY_SERVER_URL_SNAKE, url))
    }

    override suspend fun getDeviceToken(): String? =
        appConfigDao.getValor(KEY_DEVICE_TOKEN_SNAKE) ?: appConfigDao.getValor(KEY_DEVICE_TOKEN)

    override suspend fun saveDeviceToken(token: String) {
        appConfigDao.save(AppConfigEntity(KEY_DEVICE_TOKEN, token))
        appConfigDao.save(AppConfigEntity(KEY_DEVICE_TOKEN_SNAKE, token))
    }

    override suspend fun getTokenExpiry(): Long? = appConfigDao.getValor(KEY_TOKEN_EXPIRY)?.toLongOrNull()
    override suspend fun saveTokenExpiry(expiry: Long) = appConfigDao.save(AppConfigEntity(KEY_TOKEN_EXPIRY, expiry.toString()))

    override suspend fun getPairingCode(): String? = appConfigDao.getValor(KEY_PAIRING_CODE)
    override suspend fun savePairingCode(code: String) = appConfigDao.save(AppConfigEntity(KEY_PAIRING_CODE, code))

    override suspend fun getLicenseKey(): String? = appConfigDao.getValor(KEY_LICENSE_KEY)
    override suspend fun saveLicenseKey(key: String) = appConfigDao.save(AppConfigEntity(KEY_LICENSE_KEY, key))

    override suspend fun getLicenseStatus(): String? = appConfigDao.getValor(KEY_LICENSE_STATUS)
    override suspend fun saveLicenseStatus(status: String) = appConfigDao.save(AppConfigEntity(KEY_LICENSE_STATUS, status))

    override fun observeLicenseStatus(): Flow<String?> = appConfigDao.observeValor(KEY_LICENSE_STATUS)
    override fun observeFurgonPlate(): Flow<String?> = appConfigDao.observeValor(KEY_FURGON_PLATE)

    override suspend fun getModbusSimulationEnabled(): Boolean = appConfigDao.getValor(KEY_MODBUS_SIMULATION_ENABLED)?.toBooleanStrictOrNull() ?: false
    override suspend fun saveModbusSimulationEnabled(enabled: Boolean) = appConfigDao.save(AppConfigEntity(KEY_MODBUS_SIMULATION_ENABLED, enabled.toString()))

    override suspend fun getString(key: String): String? = appConfigDao.getValor(key)
    override suspend fun putString(key: String, value: String) {
        appConfigDao.save(AppConfigEntity(key, value))
        if (key == KEY_DEVICE_ID || key == KEY_DEVICE_ID_SNAKE) {
            appConfigDao.save(AppConfigEntity(clave = value, valor = "DEVICE_ID_ANCHOR"))
        }
    }

    override suspend fun getLong(key: String): Long? = appConfigDao.getValor(key)?.toLongOrNull()
    override suspend fun putLong(key: String, value: Long) = appConfigDao.save(AppConfigEntity(key, value.toString()))

    override suspend fun remove(key: String) = appConfigDao.deleteByClave(key)
}
