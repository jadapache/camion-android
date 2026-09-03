package com.pronavicola.camion

import android.app.Application
import com.pronavicola.camion.data.local.FurgonesDatabase
import com.pronavicola.camion.data.modbus.ModbusClient
import com.pronavicola.camion.data.modbus.ModbusTcpClient
import com.pronavicola.camion.data.mqtt.MqttClientManager
import com.pronavicola.camion.data.mqtt.OutboxQueueManager
import com.pronavicola.camion.data.mqtt.TelemetryMqttCoordinator
import com.pronavicola.camion.data.repository.AuthRepository
import com.pronavicola.camion.data.repository.ParametrosRepository
import com.pronavicola.camion.data.repository.RegistrosRepository
import com.pronavicola.camion.data.repository.AppConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class CamionApplication : Application() {
    val appScope = CoroutineScope(SupervisorJob())

    val database: FurgonesDatabase by lazy { FurgonesDatabase.create(this) }
    val modbusClient: ModbusClient by lazy { ModbusTcpClient(appScope) }

    val mqttClientManager: MqttClientManager by lazy { MqttClientManager(appScope) }
    val outboxQueueManager: OutboxQueueManager by lazy { OutboxQueueManager(mqttClientManager, appScope) }
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

    val screensApiService: com.pronavicola.camion.data.remote.ScreensApiService by lazy {
        com.pronavicola.camion.data.remote.ScreensApiService()
    }

    val devicePairingRepository: com.pronavicola.camion.data.repository.DevicePairingRepository by lazy {
        com.pronavicola.camion.data.repository.DevicePairingRepository(this, screensApiService, appConfigRepository)
    }

    override fun onCreate() {
        super.onCreate()
        mqttClientManager.start()
        telemetryMqttCoordinator.start()
    }
}
