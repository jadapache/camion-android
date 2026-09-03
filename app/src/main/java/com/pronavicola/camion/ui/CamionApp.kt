package com.pronavicola.camion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pronavicola.camion.CamionApplication
import com.pronavicola.camion.domain.model.AppDestination
import com.pronavicola.camion.ui.components.CustomNavBar
import com.pronavicola.camion.ui.components.UserSessionDialog
import com.pronavicola.camion.ui.config.ConfigScreen
import com.pronavicola.camion.ui.hmi.HmiScreen
import com.pronavicola.camion.ui.parametros.ParametrosScreen
import com.pronavicola.camion.ui.registros.RegistrosScreen

@Composable
fun CamionApp(app: CamionApplication) {
    val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory(app))
    val uiState by viewModel.uiState.collectAsState()
    val destinations by viewModel.visibleDestinations.collectAsState()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppDestination.HMI.route

    var showUserDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.statusMessage) {
        val message = uiState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message).let { result ->
            if (result == SnackbarResult.Dismissed) {
                viewModel.clearMessage()
            }
        }
        viewModel.clearMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CustomNavBar(
                destinations = destinations,
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                currentUser = uiState.currentUser,
                onUserClick = { showUserDialog = true },
            )

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                NavHost(
                    navController = navController,
                    startDestination = AppDestination.HMI.route,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable(AppDestination.HMI.route) {
                        HmiScreen(
                            state = uiState.hmiState,
                        )
                    }
                    composable(AppDestination.REGISTROS.route) {
                        RegistrosScreen(
                            registros = uiState.registros,
                        )
                    }
                    composable(AppDestination.PARAMETROS.route) {
                        ParametrosScreen(
                            parametros = uiState.parametros,
                            onValueChange = viewModel::updateParametro,
                        )
                    }
                    composable(AppDestination.CONFIG.route) {
                        val user = uiState.currentUser
                        if (user != null) {
                            ConfigScreen(
                                currentUser = user,
                                deviceId = uiState.deviceId,
                                mqttBrokerUrl = uiState.mqttBrokerUrl,
                                mqttTopicRoot = uiState.mqttTopicRoot,
                                mqttUsername = uiState.mqttUsername,
                                mqttPassword = uiState.mqttPassword,
                                modbusHost = uiState.modbusHost,
                                modbusPort = uiState.modbusPort,
                                publishIntervalS = uiState.publishIntervalS,
                                mqttConnected = uiState.mqttConnected,
                                modbusConnected = uiState.modbusConnected,
                                outboxPendingCount = uiState.outboxPendingCount,
                                serverBaseUrl = uiState.serverBaseUrl,
                                pairingCode = uiState.pairingCode,
                                licenseStatus = uiState.licenseStatus,
                                licenseKeyInput = uiState.licenseKeyInput,
                                isPollingPairing = uiState.isPollingPairing,
                                availableFurgones = uiState.availableFurgones,
                                onDeviceIdChange = viewModel::onDeviceIdChange,
                                onMqttBrokerUrlChange = viewModel::onMqttBrokerUrlChange,
                                onMqttTopicRootChange = viewModel::onMqttTopicRootChange,
                                onMqttUsernameChange = viewModel::onMqttUsernameChange,
                                onMqttPasswordChange = viewModel::onMqttPasswordChange,
                                onModbusHostChange = viewModel::onModbusHostChange,
                                onModbusPortChange = viewModel::onModbusPortChange,
                                onPublishIntervalChange = viewModel::onPublishIntervalChange,
                                onServerBaseUrlChange = viewModel::onServerBaseUrlChange,
                                onLicenseKeyInputChange = viewModel::onLicenseKeyInputChange,
                                onRequestPairingCode = viewModel::requestPairingCode,
                                onCheckPairingStatus = viewModel::checkPairingStatus,
                                onActivateLicenseKey = viewModel::activateLicenseKey,
                                onFetchFurgones = viewModel::fetchAvailableFurgones,
                                onSelectFurgon = viewModel::selectAndAssignFurgon,
                                onSaveSystemSettings = viewModel::saveSystemSettings,
                                onSyncAllRegistros = viewModel::syncAllRegistros,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showUserDialog) {
        val user = uiState.currentUser
        if (user != null) {
            UserSessionDialog(
                currentUser = user,
                users = uiState.availableUsers,
                selectedUserId = uiState.selectedUserId,
                loginPassword = uiState.loginPassword,
                newPassword = uiState.newPassword,
                showChangePassword = uiState.showChangePassword,
                onUserSelected = viewModel::onUserSelected,
                onLoginPasswordChange = viewModel::onLoginPasswordChange,
                onNewPasswordChange = viewModel::onNewPasswordChange,
                onToggleChangePassword = viewModel::toggleChangePassword,
                onConfirm = {
                    viewModel.confirmLogin()
                    showUserDialog = false
                },
                onDismiss = { showUserDialog = false },
            )
        }
    }
}
