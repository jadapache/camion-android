package com.pronavicola.camion

import com.pronavicola.camion.data.local.FurgonesDatabase
import com.pronavicola.camion.data.local.createFurgonesDatabase
import com.pronavicola.camion.data.modbus.ModbusClient
import com.pronavicola.camion.data.modbus.ModbusTcpClient
import com.pronavicola.camion.data.mqtt.MqttClientManager
import com.pronavicola.camion.data.mqtt.OutboxQueueManager
import com.pronavicola.camion.data.mqtt.TelemetryMqttCoordinator
import com.pronavicola.camion.data.remote.ScreensApiService
import com.pronavicola.camion.data.repository.AppConfigRepository
import com.pronavicola.camion.data.repository.AuthRepository
import com.pronavicola.camion.data.repository.DevicePairingRepository
import com.pronavicola.camion.data.repository.ParametrosRepository
import com.pronavicola.camion.data.repository.RegistrosRepository
import com.pronavicola.camion.util.PlatformInfoProvider
import com.pronavicola.camion.util.getPlatformInfoProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class CamionAppContext(
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob()),
    platformContext: Any? = null,
) {
    val database: FurgonesDatabase by lazy {
        createFurgonesDatabase(platformContext)
    }

    val modbusClient: ModbusClient by lazy {
        ModbusTcpClient(appScope)
    }

    val mqttClientManager: MqttClientManager by lazy {
        MqttClientManager(appScope)
    }

    val outboxQueueManager: OutboxQueueManager by lazy {
        OutboxQueueManager(mqttClientManager, appScope)
    }

    val telemetryMqttCoordinator: TelemetryMqttCoordinator by lazy {
        TelemetryMqttCoordinator(modbusClient, outboxQueueManager, appScope)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(database.usuarioDao())
    }

    val parametrosRepository: ParametrosRepository by lazy {
        ParametrosRepository(database.parametroDao(), modbusClient)
    }

    val registrosRepository: RegistrosRepository by lazy {
        RegistrosRepository(database.variableRegistroDao())
    }

    val appConfigRepository: AppConfigRepository by lazy {
        AppConfigRepository(database.appConfigDao())
    }

    val screensApiService: ScreensApiService by lazy {
        ScreensApiService()
    }

    val platformInfoProvider: PlatformInfoProvider by lazy {
        getPlatformInfoProvider(platformContext)
    }

    val devicePairingRepository: DevicePairingRepository by lazy {
        DevicePairingRepository(platformInfoProvider, screensApiService, appConfigRepository)
    }

    fun startBackgroundServices() {
        mqttClientManager.start()
        telemetryMqttCoordinator.start()
    }
}
