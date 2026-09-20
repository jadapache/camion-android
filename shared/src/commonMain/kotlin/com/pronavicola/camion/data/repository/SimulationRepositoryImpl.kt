package com.pronavicola.camion.data.repository

import com.pronavicola.camion.data.modbus.simulation.ModbusPlcServer
import com.pronavicola.camion.domain.model.SimulationConfig
import com.pronavicola.camion.domain.model.SimulationState
import com.pronavicola.camion.domain.repository.SimulationRepository
import kotlinx.coroutines.flow.StateFlow

class SimulationRepositoryImpl(
    private val plcServer: ModbusPlcServer,
) : SimulationRepository {
    override val state: StateFlow<SimulationState> = plcServer.state

    override fun start(config: SimulationConfig) {
        plcServer.applyConfig(config)
        plcServer.start()
    }

    override fun stop() {
        plcServer.stop()
    }

    override fun updateConfig(config: SimulationConfig) {
        plcServer.applyConfig(config)
    }

    override fun setGateOpen(open: Boolean) {
        val currentConfig = state.value.config
        updateConfig(currentConfig.copy(gateOpen = open))
    }

    override fun injectAlarm(alarmBit: Int, active: Boolean) {
        plcServer.injectAlarmBit(alarmBit, active)
    }
}
