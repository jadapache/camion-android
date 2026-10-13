package com.pronavicola.camion.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pronavicola.camion.data.repository.AlarmaRegistroRepositoryImpl
import com.pronavicola.camion.domain.usecase.alarmas.ObserveAlarmasRegistrosUseCase
import com.pronavicola.camion.presentation.alarmas.AlarmasRegistrosViewModel
import com.pronavicola.camion.presentation.auth.AuthViewModel
import com.pronavicola.camion.presentation.config.ConfigViewModel
import com.pronavicola.camion.presentation.hmi.HmiViewModel
import com.pronavicola.camion.presentation.parametros.ParametrosViewModel
import com.pronavicola.camion.presentation.registros.RegistrosViewModel
import com.pronavicola.camion.presentation.simulation.SimulationViewModel
import kotlin.reflect.KClass

/**
 * Multiplatform ViewModel Factory powered by the [AppContainer].
 */
class CamionViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
        return when (modelClass) {
            AuthViewModel::class -> AuthViewModel(
                getActiveUsersUseCase = appContainer.getActiveUsersUseCase,
                loginUseCase = appContainer.loginUseCase,
                changePasswordUseCase = appContainer.changePasswordUseCase,
                checkAccessPermissionUseCase = appContainer.checkAccessPermissionUseCase,
            ) as T

            HmiViewModel::class -> HmiViewModel(
                observeHmiStateUseCase = appContainer.observeHmiStateUseCase,
                startProcessUseCase = appContainer.startProcessUseCase,
                stopProcessUseCase = appContainer.stopProcessUseCase,
                connectModbusUseCase = appContainer.connectModbusUseCase,
                disconnectModbusUseCase = appContainer.disconnectModbusUseCase,
                appConfigRepository = appContainer.appConfigRepository,
                updateTruckPlateUseCase = appContainer.updateTruckPlateUseCase,
                devicePairingRepository = appContainer.devicePairingRepository,
                vehiculoDao = appContainer.vehiculoDao,
            ) as T

            ParametrosViewModel::class -> ParametrosViewModel(
                observeParametrosUseCase = appContainer.observeParametrosUseCase,
                updateParametroUseCase = appContainer.updateParametroUseCase,
            ) as T

            RegistrosViewModel::class -> RegistrosViewModel(
                observeRegistrosUseCase = appContainer.observeRegistrosUseCase,
                resendRegistroUseCase = appContainer.resendRegistroUseCase,
                clearAllRegistrosUseCase = appContainer.clearAllRegistrosUseCase,
                exportRegistrosCsvUseCase = appContainer.exportRegistrosCsvUseCase,
                buildTelemetryPayloadUseCase = appContainer.buildTelemetryPayloadUseCase,
                appConfigRepository = appContainer.appConfigRepository,
                getRegistrosPagedUseCase = appContainer.getRegistrosPagedUseCase,
            ) as T

            AlarmasRegistrosViewModel::class -> AlarmasRegistrosViewModel(
                observeAlarmasRegistrosUseCase = appContainer.observeAlarmasRegistrosUseCase,
                clearAllAlarmasRegistrosUseCase = appContainer.clearAllAlarmasRegistrosUseCase,
                exportAlarmasCsvUseCase = appContainer.exportAlarmasCsvUseCase,
                deviceIdStr = ""
            ) as T

            ConfigViewModel::class -> ConfigViewModel(
                getAppConfigUseCase = appContainer.getAppConfigUseCase,
                saveAppConfigUseCase = appContainer.saveAppConfigUseCase,
                testMqttConnectionUseCase = appContainer.testMqttConnectionUseCase,
                restartMqttClientUseCase = appContainer.restartMqttClientUseCase,
                pairDeviceUseCase = appContainer.pairDeviceUseCase,
                connectModbusUseCase = appContainer.connectModbusUseCase,
                updateTruckPlateUseCase = appContainer.updateTruckPlateUseCase,
                appConfigRepository = appContainer.appConfigRepository,
            ) as T

            SimulationViewModel::class -> SimulationViewModel(
                getSimulationStateUseCase = appContainer.getSimulationStateUseCase,
                toggleSimulationUseCase = appContainer.toggleSimulationUseCase,
                updateSimulationParamsUseCase = appContainer.updateSimulationParamsUseCase,
                injectSimulatedFaultUseCase = appContainer.injectSimulatedFaultUseCase,
                connectModbusUseCase = appContainer.connectModbusUseCase,
                appConfigRepository = appContainer.appConfigRepository,
            ) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.simpleName}")
        }
    }
}
