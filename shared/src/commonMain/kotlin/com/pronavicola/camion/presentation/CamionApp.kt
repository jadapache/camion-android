package com.pronavicola.camion.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pronavicola.camion.CamionAppContext
import com.pronavicola.camion.di.CamionViewModelFactory
import com.pronavicola.camion.domain.model.AppDestination
import com.pronavicola.camion.domain.model.MqttLogEvent
import com.pronavicola.camion.presentation.alarmas.AlarmasRegistrosScreen
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
import com.pronavicola.camion.presentation.hmi.components.TruckAssignmentDialog
import com.pronavicola.camion.presentation.parametros.ParametrosScreen
import com.pronavicola.camion.presentation.registros.RegistrosScreen
import com.pronavicola.camion.presentation.theme.CamionColors
import kotlin.time.Duration.Companion.milliseconds

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

    LaunchedEffect(Unit) {
        configViewModel.fetchAvailableFurgones()
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

    // Reload config when navigating to Config route, reload plate when navigating to HMI route
    LaunchedEffect(currentRoute) {
        if (currentRoute == AppDestination.CONFIG.route) {
            configViewModel.loadConfig()
        } else if (currentRoute == AppDestination.HMI.route) {
            hmiViewModel.reloadPlate()
        }
    }

    // Keep HMI plate updated dynamically whenever config plate changes
    LaunchedEffect(configState.furgonPlate) {
        val plate = configState.furgonPlate.trim()
        if (plate.isNotBlank()) {
            hmiViewModel.updatePlate(plate)
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
    LaunchedEffect(configState.pairingError) {
        configState.pairingError?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }
    LaunchedEffect(configState.pairingSuccess) {
        configState.pairingSuccess?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }
    LaunchedEffect(configState.testMqttResult) {
        configState.testMqttResult?.let { msg ->
            if (configState.testMqttSuccess != true) {
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    var isNightModeEnabled by remember { mutableStateOf(false) }
    var currentTimeString by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        while (isActive) {
            currentTimeString = formatter.format(Date())
            delay(1000L.milliseconds)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxSize(),
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
                    // Toolbar Global
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .background(CamionColors.headerBackground)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentTimeString,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = { isNightModeEnabled = !isNightModeEnabled },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isNightModeEnabled) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Alternar Modo Nocturno",
                                tint = Color.White
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (currentRoute) {
                            AppDestination.HMI.route -> {
                            HmiScreen(
                                state = hmiState.hmiState,
                                currentUser = authState.currentUser,
                                onEditTruckPlate = { hmiViewModel.openTruckAssignmentDialog() },
                            )
                        }
                        AppDestination.REGISTROS.route -> {
                            RegistrosScreen(
                                currentUser = authState.currentUser,
                                registros = registrosState.registros,
                                deviceId = registrosState.deviceId,
                                onClearAllRegistros = { authState.currentUser?.let { registrosViewModel.clearAllRegistros(it) } },
                                onExportCsv = { fileName, content ->
                                    appContext.platformCsvSharer.shareCsv(fileName, content)
                                },
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
                        AppDestination.ALARMAS_REGISTROS.route -> {
                            AlarmasRegistrosScreen(
                                viewModel = viewModel(
                                    factory = CamionViewModelFactory(appContext),
                                ),
                                currentUser = authState.currentUser,
                                onExportCsv = { fileName, content ->
                                    appContext.platformCsvSharer.shareCsv(fileName, content)
                                },
                            )
                        }
                        AppDestination.CONFIG.route -> {
                            val user = authState.currentUser ?: com.pronavicola.camion.presentation.auth.DEFAULT_SESSION_USER
                            ConfigScreen(
                                currentUser = user,
                                deviceId = configState.deviceId,
                                mqttBrokerUrl = configState.brokerUrl,
                                mqttTopicRoot = configState.topicRoot,
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
                                licenseKeyInput = configState.licenseKey,
                                isPollingPairing = configState.isPairing,
                                pairingError = configState.pairingError,
                                pairingSuccess = configState.pairingSuccess,
                                modbusSimulationEnabled = false,
                                mqttTransmittedPacketsCount = 0,
                                mqttTestSuccessMessage = configState.testMqttResult,
                                mqttTestPayloadJson = configState.testMqttPayloadJson,
                                mqttTestIsSuccess = configState.testMqttSuccess ?: false,
                                mqttEventLogs = emptyList(),
                                isTestingMqttConnection = configState.isTestingMqtt,
                                onDeviceIdChange = { id -> configViewModel.onDeviceIdChange(id) },
                                onMqttBrokerUrlChange = configViewModel::onBrokerUrlChange,
                                onMqttTopicRootChange = configViewModel::onTopicRootChange,
                                onMqttUsernameChange = configViewModel::onBrokerUsernameChange,
                                onMqttPasswordChange = configViewModel::onBrokerPasswordChange,
                                onModbusHostChange = configViewModel::onModbusHostChange,
                                onModbusPortChange = configViewModel::onModbusPortChange,
                                onPublishIntervalChange = configViewModel::onPublishIntervalChange,
                                onModbusSimulationToggle = {},
                                onServerBaseUrlChange = configViewModel::onServerBaseUrlChange,
                                onLicenseKeyInputChange = configViewModel::onLicenseKeyInputChange,
                                onRequestPairingCode = configViewModel::requestPairingCode,
                                onCheckPairingStatus = configViewModel::checkPairingStatus,
                                onActivateLicenseKey = configViewModel::activateLicenseKey,
                                onUnlinkDevice = configViewModel::unlinkDevice,
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

        // Modo Nocturno Overlay
        if (isNightModeEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
            ) {
                // Toolbar transparente encima del overlay para apagarlo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentTimeString,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = { isNightModeEnabled = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LightMode,
                            contentDescription = "Desactivar Modo Nocturno",
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                // Métricas minimalistas en texto esquina inferior derecha
                val t1 = hmiState.hmiState.temperatura1 ?: 0.0
                val t2 = hmiState.hmiState.temperatura2 ?: 0.0
                val hum = hmiState.hmiState.humedad ?: 0.0
                val tension = hmiState.hmiState.tension ?: 0.0
                
                val t1Str = "%.1f".format(t1)
                val t2Str = "%.1f".format(t2)
                val humStr = "%.1f".format(hum)
                val tenStr = "%.1f".format(tension)

                Text(
                    text = "T1: $t1Str°C / T2: $t2Str°C / H: $humStr % / Tensión: $tenStr VDC",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                )
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

    if (hmiState.showTruckAssignmentDialog) {
        TruckAssignmentDialog(
            currentPlate = hmiState.furgonPlate,
            availableFurgones = hmiState.availableFurgones,
            isLoading = hmiState.isLoadingFurgones,
            errorMessage = hmiState.furgonAssignmentError,
            onDismiss = { hmiViewModel.closeTruckAssignmentDialog() },
            onConfirmPlate = { plate ->
                hmiViewModel.assignTruckPlate(plate)
                configViewModel.updateAssignedPlate(plate)
                configViewModel.loadConfig()
            },
            onRefreshFurgones = { hmiViewModel.fetchAvailableFurgones() },
        )
    }
}
}
