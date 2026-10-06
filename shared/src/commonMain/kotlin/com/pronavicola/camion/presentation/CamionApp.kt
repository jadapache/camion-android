package com.pronavicola.camion.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.CreationExtras
import com.pronavicola.camion.CamionAppContext
import com.pronavicola.camion.di.CamionViewModelFactory
import com.pronavicola.camion.domain.model.AppDestination
import com.pronavicola.camion.domain.model.MqttLogEvent
import com.pronavicola.camion.presentation.auth.AuthViewModel
import com.pronavicola.camion.presentation.config.ConfigViewModel
import com.pronavicola.camion.presentation.hmi.HmiViewModel
import com.pronavicola.camion.presentation.parametros.ParametrosViewModel
import com.pronavicola.camion.presentation.registros.RegistrosViewModel
import com.pronavicola.camion.presentation.simulation.SimulationScreen
import com.pronavicola.camion.presentation.simulation.SimulationViewModel
import com.pronavicola.camion.presentation.navigation.CustomNavBar
import com.pronavicola.camion.presentation.auth.components.UserSessionDialog
import com.pronavicola.camion.presentation.config.ConfigScreen
import com.pronavicola.camion.presentation.hmi.HmiScreen
import com.pronavicola.camion.presentation.parametros.ParametrosScreen
import com.pronavicola.camion.presentation.registros.RegistrosScreen

@Composable
fun CamionApp(
    appContext: CamionAppContext,
    modifier: Modifier = Modifier,
) {
    val factory = remember(appContext) { CamionViewModelFactory(appContext) }

    val authViewModel = remember(appContext) {
        factory.create(AuthViewModel::class, CreationExtras.Empty)
    }
    val hmiViewModel = remember(appContext) {
        factory.create(HmiViewModel::class, CreationExtras.Empty)
    }
    val parametrosViewModel = remember(appContext) {
        factory.create(ParametrosViewModel::class, CreationExtras.Empty)
    }
    val registrosViewModel = remember(appContext) {
        factory.create(RegistrosViewModel::class, CreationExtras.Empty)
    }
    val configViewModel = remember(appContext) {
        factory.create(ConfigViewModel::class, CreationExtras.Empty)
    }
    val simulationViewModel = remember(appContext) {
        factory.create(SimulationViewModel::class, CreationExtras.Empty)
    }

    val authState by authViewModel.uiState.collectAsState()
    val hmiState by hmiViewModel.uiState.collectAsState()
    val parametrosState by parametrosViewModel.uiState.collectAsState()
    val registrosState by registrosViewModel.uiState.collectAsState()
    val configState by configViewModel.uiState.collectAsState()

    val modbusConnected by appContext.modbusRepository.isConnected.collectAsState()
    val mqttConnected by appContext.mqttRepository.isConnected.collectAsState()
    val outboxPendingCount by appContext.mqttRepository.outboxPendingCount.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var currentRoute by remember { mutableStateOf(AppDestination.HMI.route) }

    // Sync active user to Parametros and Registros ViewModels
    LaunchedEffect(authState.currentUser) {
        authState.currentUser?.let { user ->
            parametrosViewModel.loadParametros(user)
            registrosViewModel.loadRegistros(user)
        }
    }

    // Dynamic visible destinations based on role
    val visibleDestinations = remember(authState.currentUser) {
        val user = authState.currentUser
        if (user == null) {
            listOf(
                AppDestination.HMI,
                AppDestination.REGISTROS,
                AppDestination.PARAMETROS,
            )
        } else {
            AppDestination.entries
                .filter { dest ->
                    dest.showInNavBar && authViewModel.canAccessScreen(dest.accessLevel)
                }
        }
    }

    // Adjust route if current becomes unauthorized
    LaunchedEffect(visibleDestinations) {
        if (visibleDestinations.none { it.route == currentRoute }) {
            currentRoute = AppDestination.HMI.route
        }
    }

    // Snackbar notifications
    LaunchedEffect(registrosState.statusMessage) {
        registrosState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            registrosViewModel.clearStatusMessage()
        }
    }
    LaunchedEffect(configState.saveMessage) {
        configState.saveMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            configViewModel.clearSaveMessage()
        }
    }
    LaunchedEffect(configState.testMqttResult) {
        configState.testMqttResult?.let { msg ->
            if (configState.testMqttSuccess != true) {
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier,
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            CustomNavBar(
                destinations = visibleDestinations,
                currentRoute = currentRoute,
                onNavigate = { route -> currentRoute = route },
                currentUser = authState.currentUser,
                onUserClick = { authViewModel.openUserDialog() },
            )

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentRoute) {
                        AppDestination.HMI.route -> {
                            HmiScreen(
                                state = hmiState.hmiState,
                            )
                        }
                        AppDestination.REGISTROS.route -> {
                            RegistrosScreen(
                                currentUser = authState.currentUser,
                                registros = registrosState.registros,
                                deviceId = registrosState.deviceId,
                                onResendRegistro = { registro -> registrosViewModel.resendRegistro(registro) },
                                onClearAllRegistros = { authState.currentUser?.let { registrosViewModel.clearAllRegistros(it) } },
                            )
                        }
                        AppDestination.PARAMETROS.route -> {
                            ParametrosScreen(
                                parametros = parametrosState.parametros,
                                onValueChange = { param, value ->
                                    authState.currentUser?.let { user ->
                                        parametrosViewModel.startEditing(param)
                                        parametrosViewModel.onEditValueChanged(value.toString())
                                        parametrosViewModel.saveParameter(user)
                                    }
                                },
                            )
                        }
                        AppDestination.CONFIG.route -> {
                            val user = authState.currentUser ?: com.pronavicola.camion.presentation.auth.DEFAULT_SESSION_USER
                            ConfigScreen(
                                currentUser = user,
                                deviceId = configState.deviceId,
                                furgonPlate = configState.furgonPlate,
                                mqttBrokerUrl = configState.brokerUrl,
                                mqttTopicRoot = "furgones",
                                mqttUsername = configState.brokerUsername,
                                mqttPassword = configState.brokerPassword,
                                modbusHost = configState.modbusHost,
                                modbusPort = configState.modbusPort,
                                publishIntervalS = configState.publishInterval,
                                mqttConnected = mqttConnected,
                                modbusConnected = modbusConnected,
                                outboxPendingCount = outboxPendingCount,
                                serverBaseUrl = configState.serverBaseUrl,
                                pairingCode = configState.pairingCode,
                                licenseStatus = configState.licenseStatus,
                                licenseKeyInput = "",
                                isPollingPairing = configState.isPairing,
                                availableFurgones = emptyList(),
                                modbusSimulationEnabled = false,
                                mqttTransmittedPacketsCount = 0,
                                mqttTestSuccessMessage = configState.testMqttResult,
                                mqttTestPayloadJson = configState.testMqttPayloadJson,
                                mqttTestIsSuccess = configState.testMqttSuccess ?: false,
                                mqttEventLogs = emptyList<MqttLogEvent>(),
                                isTestingMqttConnection = configState.isTestingMqtt,
                                onDeviceIdChange = configViewModel::onDeviceIdChange,
                                onFurgonPlateChange = configViewModel::onFurgonPlateChange,
                                onMqttBrokerUrlChange = configViewModel::onBrokerUrlChange,
                                onMqttTopicRootChange = {},
                                onMqttUsernameChange = configViewModel::onBrokerUsernameChange,
                                onMqttPasswordChange = configViewModel::onBrokerPasswordChange,
                                onModbusHostChange = configViewModel::onModbusHostChange,
                                onModbusPortChange = configViewModel::onModbusPortChange,
                                onPublishIntervalChange = configViewModel::onPublishIntervalChange,
                                onModbusSimulationToggle = {},
                                onServerBaseUrlChange = {},
                                onLicenseKeyInputChange = {},
                                onRequestPairingCode = configViewModel::requestPairingCode,
                                onCheckPairingStatus = configViewModel::checkPairingStatus,
                                onActivateLicenseKey = {},
                                onFetchFurgones = {},
                                onSelectFurgon = {},
                                onUnlinkDevice = {},
                                onTestMqttConnection = configViewModel::testMqttConnection,
                                onNavigateToSimulation = { currentRoute = AppDestination.SIMULACION.route },
                                onSaveSystemSettings = {
                                    configViewModel.saveMqttConfig()
                                    configViewModel.saveModbusConfig()
                                    configViewModel.saveSystemSettings()
                                },
                                onSyncAllRegistros = {},
                            )
                        }
                        AppDestination.SIMULACION.route -> {
                            SimulationScreen(
                                viewModel = simulationViewModel,
                                onNavigateToConfig = { currentRoute = AppDestination.CONFIG.route },
                            )
                        }
                    }
                }
            }
        }
    }

    if (authState.showUserDialog) {
        val user = authState.currentUser ?: com.pronavicola.camion.presentation.auth.DEFAULT_SESSION_USER
        var selectedUser by remember(user) { mutableStateOf(user.idUsuario) }
        UserSessionDialog(
            currentUser = user,
            users = authState.activeUsers,
            selectedUserId = selectedUser,
            loginPassword = authState.loginPasswordInput,
            currentPassword = authState.currentPasswordInput,
            newPassword = authState.newPasswordInput,
            showChangePassword = authState.isChangePasswordMode,
            errorMessage = authState.errorMessage,
            onUserSelected = { selectedUser = it },
            onLoginPasswordChange = authViewModel::onLoginPasswordChange,
            onCurrentPasswordChange = authViewModel::onCurrentPasswordChange,
            onNewPasswordChange = authViewModel::onNewPasswordChange,
            onToggleChangePassword = { authViewModel.setPasswordMode(!authState.isChangePasswordMode) },
            onConfirm = {
                if (authState.isChangePasswordMode) {
                    authViewModel.changePassword()
                } else {
                    authViewModel.switchUser(selectedUser)
                }
            },
            onDismiss = { authViewModel.closeUserDialog() },
        )
    }
}
