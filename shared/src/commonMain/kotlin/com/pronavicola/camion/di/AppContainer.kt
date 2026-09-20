package com.pronavicola.camion.di

import com.pronavicola.camion.data.local.FurgonesDatabase
import com.pronavicola.camion.data.local.createFurgonesDatabase
import com.pronavicola.camion.data.modbus.ModbusClient
import com.pronavicola.camion.data.modbus.ModbusTcpClient
import com.pronavicola.camion.data.modbus.simulation.ModbusPlcServer
import com.pronavicola.camion.data.mqtt.MqttClientManager
import com.pronavicola.camion.data.mqtt.OutboxQueueManager
import com.pronavicola.camion.data.mqtt.TelemetryMqttCoordinator
import com.pronavicola.camion.data.remote.ScreensApiService
import com.pronavicola.camion.data.repository.AppConfigRepositoryImpl
import com.pronavicola.camion.data.repository.AuthRepositoryImpl
import com.pronavicola.camion.data.repository.DevicePairingRepositoryImpl
import com.pronavicola.camion.data.repository.ModbusRepositoryImpl
import com.pronavicola.camion.data.repository.MqttRepositoryImpl
import com.pronavicola.camion.data.repository.ParametrosRepositoryImpl
import com.pronavicola.camion.data.repository.RegistrosRepositoryImpl
import com.pronavicola.camion.data.repository.SimulationRepositoryImpl
import com.pronavicola.camion.domain.repository.AppConfigRepository
import com.pronavicola.camion.domain.repository.AuthRepository
import com.pronavicola.camion.domain.repository.DevicePairingRepository
import com.pronavicola.camion.domain.repository.ModbusRepository
import com.pronavicola.camion.domain.repository.MqttRepository
import com.pronavicola.camion.domain.repository.ParametrosRepository
import com.pronavicola.camion.domain.repository.RegistrosRepository
import com.pronavicola.camion.domain.repository.SimulationRepository
import com.pronavicola.camion.domain.usecase.auth.ChangePasswordUseCase
import com.pronavicola.camion.domain.usecase.auth.CheckAccessPermissionUseCase
import com.pronavicola.camion.domain.usecase.auth.GetActiveUsersUseCase
import com.pronavicola.camion.domain.usecase.auth.LoginUseCase
import com.pronavicola.camion.domain.usecase.config.GetAppConfigUseCase
import com.pronavicola.camion.domain.usecase.config.PairDeviceUseCase
import com.pronavicola.camion.domain.usecase.config.SaveAppConfigUseCase
import com.pronavicola.camion.domain.usecase.config.TestMqttConnectionUseCase
import com.pronavicola.camion.domain.usecase.config.UpdateTruckPlateUseCase
import com.pronavicola.camion.domain.usecase.hmi.ConnectModbusUseCase
import com.pronavicola.camion.domain.usecase.hmi.DisconnectModbusUseCase
import com.pronavicola.camion.domain.usecase.hmi.ObserveHmiStateUseCase
import com.pronavicola.camion.domain.usecase.hmi.StartProcessUseCase
import com.pronavicola.camion.domain.usecase.hmi.StopProcessUseCase
import com.pronavicola.camion.domain.usecase.parametros.ObserveParametrosUseCase
import com.pronavicola.camion.domain.usecase.parametros.UpdateParametroUseCase
import com.pronavicola.camion.domain.usecase.registros.BuildTelemetryPayloadUseCase
import com.pronavicola.camion.domain.usecase.registros.ClearAllRegistrosUseCase
import com.pronavicola.camion.domain.usecase.registros.ExportRegistrosCsvUseCase
import com.pronavicola.camion.domain.usecase.registros.ObserveRegistrosUseCase
import com.pronavicola.camion.domain.usecase.registros.ResendRegistroUseCase
import com.pronavicola.camion.domain.usecase.registros.SaveTelemetryRegistroUseCase
import com.pronavicola.camion.domain.usecase.simulation.GetSimulationStateUseCase
import com.pronavicola.camion.domain.usecase.simulation.InjectSimulatedFaultUseCase
import com.pronavicola.camion.domain.usecase.simulation.ToggleSimulationUseCase
import com.pronavicola.camion.domain.usecase.simulation.UpdateSimulationParamsUseCase
import com.pronavicola.camion.service.ModbusPollingCoordinator
import com.pronavicola.camion.util.PlatformInfoProvider
import com.pronavicola.camion.util.getPlatformInfoProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Enterprise Application DI Container for Kotlin Multiplatform (Android + Desktop).
 * Serves as the single root source of truth for repositories, use cases,
 * coordinators, and persistent database instances.
 */
class AppContainer(
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob()),
    platformContext: Any? = null,
) {
    // 1. Data Sources & Local Database
    val database: FurgonesDatabase by lazy {
        createFurgonesDatabase(platformContext)
    }

    val platformInfoProvider: PlatformInfoProvider by lazy {
        getPlatformInfoProvider(platformContext)
    }

    val screensApiService: ScreensApiService by lazy {
        ScreensApiService()
    }

    val alarmaDao by lazy {
        database.alarmaDao()
    }

    val alarmaRegistroDao by lazy {
        database.alarmaRegistroDao()
    }

    val definicionDao by lazy {
        database.definicionDao()
    }

    val vehiculoDao by lazy {
        database.vehiculoDao()
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

    // 2. Repositories
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(database.usuarioDao())
    }

    val appConfigRepository: AppConfigRepository by lazy {
        AppConfigRepositoryImpl(database.appConfigDao())
    }

    val modbusRepository: ModbusRepository by lazy {
        ModbusRepositoryImpl(modbusClient)
    }

    val parametrosRepository: ParametrosRepository by lazy {
        ParametrosRepositoryImpl(database.parametroDao(), database.vehiculoDao(), modbusClient)
    }

    val registrosRepository: RegistrosRepository by lazy {
        RegistrosRepositoryImpl(database.variableRegistroDao(), database.vehiculoDao())
    }

    val mqttRepository: MqttRepository by lazy {
        MqttRepositoryImpl(mqttClientManager, outboxQueueManager)
    }

    val devicePairingRepository: DevicePairingRepository by lazy {
        DevicePairingRepositoryImpl(platformInfoProvider, screensApiService, appConfigRepository)
    }

    val modbusPlcServer: ModbusPlcServer by lazy {
        ModbusPlcServer(appScope)
    }

    val simulationRepository: SimulationRepository by lazy {
        SimulationRepositoryImpl(modbusPlcServer)
    }

    // 3. Background Coordinators
    val telemetryMqttCoordinator: TelemetryMqttCoordinator by lazy {
        TelemetryMqttCoordinator(modbusClient, outboxQueueManager, appScope)
    }

    val modbusPollingCoordinator: ModbusPollingCoordinator by lazy {
        ModbusPollingCoordinator(
            modbusClient = modbusClient,
            registrosRepository = registrosRepository,
            appConfigRepository = appConfigRepository,
            telemetryMqttCoordinator = telemetryMqttCoordinator,
            mqttClientManager = mqttClientManager,
            scope = appScope,
        )
    }

    // 4. Use Cases - Auth
    val getActiveUsersUseCase: GetActiveUsersUseCase by lazy { GetActiveUsersUseCase(authRepository) }
    val loginUseCase: LoginUseCase by lazy { LoginUseCase(authRepository) }
    val changePasswordUseCase: ChangePasswordUseCase by lazy { ChangePasswordUseCase(authRepository) }
    val checkAccessPermissionUseCase: CheckAccessPermissionUseCase by lazy { CheckAccessPermissionUseCase(authRepository) }

    // 5. Use Cases - HMI
    val observeHmiStateUseCase: ObserveHmiStateUseCase by lazy { ObserveHmiStateUseCase(modbusRepository) }
    val startProcessUseCase: StartProcessUseCase by lazy { StartProcessUseCase(modbusRepository) }
    val stopProcessUseCase: StopProcessUseCase by lazy { StopProcessUseCase(modbusRepository) }
    val connectModbusUseCase: ConnectModbusUseCase by lazy { ConnectModbusUseCase(modbusRepository) }
    val disconnectModbusUseCase: DisconnectModbusUseCase by lazy { DisconnectModbusUseCase(modbusRepository) }

    // 6. Use Cases - Parametros
    val observeParametrosUseCase: ObserveParametrosUseCase by lazy { ObserveParametrosUseCase(parametrosRepository) }
    val updateParametroUseCase: UpdateParametroUseCase by lazy { UpdateParametroUseCase(parametrosRepository) }

    // 7. Use Cases - Registros
    val observeRegistrosUseCase: ObserveRegistrosUseCase by lazy { ObserveRegistrosUseCase(registrosRepository) }
    val saveTelemetryRegistroUseCase: SaveTelemetryRegistroUseCase by lazy { SaveTelemetryRegistroUseCase(registrosRepository) }
    val resendRegistroUseCase: ResendRegistroUseCase by lazy { ResendRegistroUseCase(registrosRepository, mqttRepository) }
    val clearAllRegistrosUseCase: ClearAllRegistrosUseCase by lazy { ClearAllRegistrosUseCase(registrosRepository) }
    val buildTelemetryPayloadUseCase: BuildTelemetryPayloadUseCase by lazy { BuildTelemetryPayloadUseCase() }
    val exportRegistrosCsvUseCase: ExportRegistrosCsvUseCase by lazy { ExportRegistrosCsvUseCase() }

    // 8. Use Cases - Config
    val getAppConfigUseCase: GetAppConfigUseCase by lazy { GetAppConfigUseCase(appConfigRepository) }
    val saveAppConfigUseCase: SaveAppConfigUseCase by lazy { SaveAppConfigUseCase(appConfigRepository) }
    val testMqttConnectionUseCase: TestMqttConnectionUseCase by lazy { TestMqttConnectionUseCase(mqttRepository, appConfigRepository) }
    val pairDeviceUseCase: PairDeviceUseCase by lazy { PairDeviceUseCase(devicePairingRepository) }
    val updateTruckPlateUseCase: UpdateTruckPlateUseCase by lazy {
        UpdateTruckPlateUseCase(database.vehiculoDao(), database.parametroDao(), appConfigRepository)
    }

    // 9. Use Cases - Simulation
    val getSimulationStateUseCase: GetSimulationStateUseCase by lazy { GetSimulationStateUseCase(simulationRepository) }
    val toggleSimulationUseCase: ToggleSimulationUseCase by lazy { ToggleSimulationUseCase(simulationRepository) }
    val updateSimulationParamsUseCase: UpdateSimulationParamsUseCase by lazy { UpdateSimulationParamsUseCase(simulationRepository) }
    val injectSimulatedFaultUseCase: InjectSimulatedFaultUseCase by lazy { InjectSimulatedFaultUseCase(simulationRepository) }

    /**
     * Initializes background services (MQTT client connection, polling loop, telemetry publishing).
     */
    fun startBackgroundServices() {
        appScope.launch {
            val brokerUrl = appConfigRepository.getBrokerUrl() ?: "tcp://broker.emqx.io:1883"
            val deviceId = appConfigRepository.getDeviceId() ?: "ABC123"
            val username = appConfigRepository.getBrokerUsername() ?: ""
            val password = appConfigRepository.getBrokerPassword() ?: ""
            val host = appConfigRepository.getModbusHost() ?: "127.0.0.1"
            val port = appConfigRepository.getModbusPort()?.toIntOrNull() ?: 502

            // Start MQTT
            mqttClientManager.start(brokerUrl, "camion_$deviceId", username, password)
            telemetryMqttCoordinator.start(deviceId)

            // Connect Modbus TCP
            modbusRepository.connect(host, port)

            // Start continuous sampling and SQLite persistence coordinator
            modbusPollingCoordinator.start()
        }
    }

    fun stopBackgroundServices() {
        modbusPollingCoordinator.stop()
        telemetryMqttCoordinator.stop()
        mqttClientManager.stop()
        appScope.launch {
            modbusRepository.disconnect()
        }
    }
}
