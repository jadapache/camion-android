package com.pronavicola.camion.domain.usecase.simulation

import com.pronavicola.camion.domain.model.SimulationConfig
import com.pronavicola.camion.domain.model.SimulationState
import com.pronavicola.camion.domain.repository.SimulationRepository
import kotlinx.coroutines.flow.StateFlow

class GetSimulationStateUseCase(
    private val simulationRepository: SimulationRepository,
) {
    operator fun invoke(): StateFlow<SimulationState> = simulationRepository.state
}

class ToggleSimulationUseCase(
    private val simulationRepository: SimulationRepository,
) {
    operator fun invoke(enable: Boolean, config: SimulationConfig = SimulationConfig()) {
        if (enable) {
            simulationRepository.start(config)
        } else {
            simulationRepository.stop()
        }
    }
}

class UpdateSimulationParamsUseCase(
    private val simulationRepository: SimulationRepository,
) {
    operator fun invoke(config: SimulationConfig) {
        simulationRepository.updateConfig(config)
    }

    fun setGateOpen(open: Boolean) {
        simulationRepository.setGateOpen(open)
    }
}

class InjectSimulatedFaultUseCase(
    private val simulationRepository: SimulationRepository,
) {
    operator fun invoke(alarmBit: Int, active: Boolean) {
        simulationRepository.injectAlarm(alarmBit, active)
    }
}
