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
import com.pronavicola.camion.domain.usecase.config.RestartMqttClientUseCase
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
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

    val buzonMqttDao by lazy {
        database.buzonMqttDao()
    }

    val trazasUbicacionDao by lazy {
        database.trazasUbicacionDao()
    }

    val gpsLocationTracker: com.pronavicola.camion.service.GpsLocationTracker by lazy {
        com.pronavicola.camion.service.createGpsLocationTracker(
            platformContext = platformContext,
            trazasUbicacionDao = trazasUbicacionDao,
            deviceIdProvider = { appConfigRepository.getDeviceId() ?: "AAA000" },
            scope = appScope
        )
    }

    val modbusClient: ModbusClient by lazy {
        ModbusTcpClient(appScope)
    }

    val mqttClientManager: MqttClientManager by lazy {
        MqttClientManager(appScope)
    }

    val outboxQueueManager: OutboxQueueManager by lazy {
        OutboxQueueManager(
            mqttManager = mqttClientManager,
            buzonMqttDao = buzonMqttDao,
            scope = appScope
        )
    }

    // 2. Repositories
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            usuarioDao = database.usuarioDao(),
            perfilDao = database.perfilDao(),
        )
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
            alarmaDao = database.alarmaDao(),
            alarmaRegistroDao = database.alarmaRegistroDao(),
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
    val restartMqttClientUseCase: RestartMqttClientUseCase by lazy {
        RestartMqttClientUseCase(appConfigRepository, mqttRepository)
    }
    val testMqttConnectionUseCase: TestMqttConnectionUseCase by lazy { TestMqttConnectionUseCase(appConfigRepository) }
    val pairDeviceUseCase: PairDeviceUseCase by lazy { PairDeviceUseCase(devicePairingRepository) }
    val updateTruckPlateUseCase: UpdateTruckPlateUseCase by lazy {
        UpdateTruckPlateUseCase(database.vehiculoDao(), database.parametroDao(), appConfigRepository, telemetryMqttCoordinator)
    }
    val cleanupHistoricalDataUseCase: com.pronavicola.camion.domain.usecase.config.CleanupHistoricalDataUseCase by lazy {
        com.pronavicola.camion.domain.usecase.config.CleanupHistoricalDataUseCase(database.variableRegistroDao(), database.trazasUbicacionDao())
    }

    // 9. Use Cases - Simulation
    val getSimulationStateUseCase: GetSimulationStateUseCase by lazy { GetSimulationStateUseCase(simulationRepository) }
    val toggleSimulationUseCase: ToggleSimulationUseCase by lazy { ToggleSimulationUseCase(simulationRepository) }
    val updateSimulationParamsUseCase: UpdateSimulationParamsUseCase by lazy { UpdateSimulationParamsUseCase(simulationRepository) }
    val injectSimulatedFaultUseCase: InjectSimulatedFaultUseCase by lazy { InjectSimulatedFaultUseCase(simulationRepository) }

    private var periodicPairingSyncJob: Job? = null

    /**
     * Valida periódicamente el estado de emparejamiento y vinculación con la API web.
     * Si el dispositivo cuenta con conexión a internet:
     * - Efectúa consulta del estado de vinculación (detectando si fue eliminado o revocado).
     * - Sincroniza la placa en caso de haber sido reasignada desde la web app.
     */
    private fun startPeriodicPairingValidation() {
        periodicPairingSyncJob?.cancel()
        periodicPairingSyncJob = appScope.launch {
            while (isActive) {
                delay(15_000L) // Consulta periódica cada 15 segundos
                val serverUrl = appConfigRepository.getServerBaseUrl()?.trim()
                val deviceId = appConfigRepository.getDeviceId()?.trim()
                val deviceToken = appConfigRepository.getDeviceToken()?.trim()

                if (!serverUrl.isNullOrBlank() && (!deviceId.isNullOrBlank() || !deviceToken.isNullOrBlank())) {
                    try {
                        val result = devicePairingRepository.validateScreen(serverUrl, deviceToken, deviceId)
                        if (result.isSuccess) {
                            val resp = result.getOrThrow()
                            if (resp.isValid && resp.status == "ACTIVE") {
                                appConfigRepository.saveLicenseStatus("ACTIVE")
                                val remotePlate = resp.furgonPlate ?: resp.furgon?.plate
                                val currentLocalPlate = appConfigRepository.getFurgonPlate()?.trim()
                                if (!remotePlate.isNullOrBlank() &&
                                    !remotePlate.equals(deviceId, ignoreCase = true) &&
                                    !remotePlate.equals(currentLocalPlate, ignoreCase = true)
                                ) {
                                    Log.i("AppContainer", "Reasignación de placa detectada desde web app: '$currentLocalPlate' -> '$remotePlate'")
                                    updateTruckPlateUseCase(remotePlate)
                                }
                            } else {
                                // El registro del dispositivo fue deshabilitado, revocado o puesto en pendiente en la web
                                val newStatus = when (resp.status.uppercase()) {
                                    "REVOKED" -> "REVOKED"
                                    "INACTIVE" -> "INACTIVE"
                                    "PENDING", "PENDING_PAIRING" -> "PENDING_PAIRING"
                                    else -> "UNPAIRED"
                                }
                                Log.w("AppContainer", "Dispositivo no activo en FURGN-BI. Estado reportado: $newStatus")
                                appConfigRepository.saveLicenseStatus(newStatus)
                            }
                        } else {
                            val ex = result.exceptionOrNull()
                            if (ex is ScreensApiService.ApiException && (ex.code == 404 || ex.code == 401)) {
                                Log.w("AppContainer", "El registro del dispositivo fue eliminado de la API web (${ex.message}). Marcando como NO VINCULADO.")
                                appConfigRepository.saveLicenseStatus("UNPAIRED")
                            }
                        }
                    } catch (_: Exception) {
                        // Error de red transitorio (modo offline en carretera): se preserva el estado local
                    }
                }
            }
        }
    }

    /**
     * Initializes background services (MQTT client connection, polling loop, telemetry publishing).
     */
    fun startBackgroundServices() {
        startPeriodicPairingValidation()
        appScope.launch {
            // Auto-heal DB state if deviceId was previously saved as truck plate
            val currentPlate = appConfigRepository.getFurgonPlate()
            val currentDevId = appConfigRepository.getDeviceId()
            if (!currentPlate.isNullOrBlank() && !currentDevId.isNullOrBlank() && currentPlate == currentDevId) {
                appConfigRepository.saveFurgonPlate("ABC123")
                val v = vehiculoDao.getPredeterminado() ?: vehiculoDao.getById(1)
                if (v != null && v.placa == currentDevId) {
                    vehiculoDao.update(v.copy(placa = "ABC123"))
                }
            }

            val brokerUrl = appConfigRepository.getBrokerUrl()?.trim() ?: ""
            val deviceId = appConfigRepository.getDeviceId() ?: "ABC123"
            val username = appConfigRepository.getBrokerUsername() ?: ""
            val password = appConfigRepository.getBrokerPassword() ?: ""
            val host = appConfigRepository.getModbusHost()?.trim() ?: ""
            val port = appConfigRepository.getModbusPort()?.toIntOrNull() ?: 502

            // Start MQTT only if a broker URL is configured
            if (brokerUrl.isNotBlank()) {
                mqttClientManager.start(brokerUrl, "camion_$deviceId", username, password)
            }
            telemetryMqttCoordinator.start(deviceId)

            // Connect Modbus TCP only if host is configured
            if (host.isNotBlank()) {
                modbusRepository.connect(host, port)
            }

            // Start GPS tracking
            gpsLocationTracker.startTracking()

            // Start continuous sampling and SQLite persistence coordinator
            modbusPollingCoordinator.start()
        }
    }

    fun stopBackgroundServices() {
        periodicPairingSyncJob?.cancel()
        periodicPairingSyncJob = null
        appScope.launch {
            gpsLocationTracker.stopTracking()
        }
        modbusPollingCoordinator.stop()
        telemetryMqttCoordinator.stop()
        mqttClientManager.stop()
        appScope.launch {
            modbusRepository.disconnect()
        }
    }
}
