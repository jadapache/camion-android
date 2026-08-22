package com.pronavicola.camion.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
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
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                uiState.currentUser?.let { user ->
                    Text(
                        text = "Usuario: ${user.usuario}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.SemiBold,
                    )
                }

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
                                users = uiState.availableUsers,
                                selectedUserId = uiState.selectedUserId,
                                loginPassword = uiState.loginPassword,
                                newPassword = uiState.newPassword,
                                showChangePassword = uiState.showChangePassword,
                                onUserSelected = viewModel::onUserSelected,
                                onLoginPasswordChange = viewModel::onLoginPasswordChange,
                                onNewPasswordChange = viewModel::onNewPasswordChange,
                                onToggleChangePassword = viewModel::toggleChangePassword,
                                onConfirm = viewModel::confirmLogin,
                            )
                        }
                    }
                }
            }

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
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
