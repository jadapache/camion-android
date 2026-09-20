package com.pronavicola.camion.domain.repository

import com.pronavicola.camion.domain.model.SimulationConfig
import com.pronavicola.camion.domain.model.SimulationState
import kotlinx.coroutines.flow.StateFlow

interface SimulationRepository {
    val state: StateFlow<SimulationState>
    fun start(config: SimulationConfig = SimulationConfig())
    fun stop()
    fun updateConfig(config: SimulationConfig)
    fun setGateOpen(open: Boolean)
    fun injectAlarm(alarmBit: Int, active: Boolean)
}
