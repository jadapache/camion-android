package com.pronavicola.camion.data.repository

import com.pronavicola.camion.data.local.dao.AppConfigDao
import com.pronavicola.camion.data.local.entity.AppConfigEntity

class AppConfigRepository(
    private val appConfigDao: AppConfigDao,
) {
    companion object {
        const val KEY_DEVICE_ID = "deviceId"
        const val KEY_BROKER_URL = "brokerUrl"
        const val KEY_TOPIC_ROOT = "topicRoot"
        const val KEY_BROKER_USERNAME = "brokerUsername"
        const val KEY_BROKER_PASSWORD = "brokerPassword"
        const val KEY_MODBUS_HOST = "modbusHost"
        const val KEY_MODBUS_PORT = "modbusPort"
        const val KEY_PUBLISH_INTERVAL = "publishIntervalS"
    }

    suspend fun getDeviceId(): String? = appConfigDao.getValor(KEY_DEVICE_ID)
    suspend fun saveDeviceId(id: String) = appConfigDao.save(AppConfigEntity(KEY_DEVICE_ID, id))

    suspend fun getBrokerUrl(): String? = appConfigDao.getValor(KEY_BROKER_URL)
    suspend fun saveBrokerUrl(url: String) = appConfigDao.save(AppConfigEntity(KEY_BROKER_URL, url))

    suspend fun getTopicRoot(): String? = appConfigDao.getValor(KEY_TOPIC_ROOT)
    suspend fun saveTopicRoot(root: String) = appConfigDao.save(AppConfigEntity(KEY_TOPIC_ROOT, root))

    suspend fun getBrokerUsername(): String? = appConfigDao.getValor(KEY_BROKER_USERNAME)
    suspend fun saveBrokerUsername(username: String) = appConfigDao.save(AppConfigEntity(KEY_BROKER_USERNAME, username))

    suspend fun getBrokerPassword(): String? = appConfigDao.getValor(KEY_BROKER_PASSWORD)
    suspend fun saveBrokerPassword(password: String) = appConfigDao.save(AppConfigEntity(KEY_BROKER_PASSWORD, password))

    suspend fun getModbusHost(): String? = appConfigDao.getValor(KEY_MODBUS_HOST)
    suspend fun saveModbusHost(host: String) = appConfigDao.save(AppConfigEntity(KEY_MODBUS_HOST, host))

    suspend fun getModbusPort(): String? = appConfigDao.getValor(KEY_MODBUS_PORT)
    suspend fun saveModbusPort(port: String) = appConfigDao.save(AppConfigEntity(KEY_MODBUS_PORT, port))

    suspend fun getPublishInterval(): String? = appConfigDao.getValor(KEY_PUBLISH_INTERVAL)
    suspend fun savePublishInterval(interval: String) = appConfigDao.save(AppConfigEntity(KEY_PUBLISH_INTERVAL, interval))
}
