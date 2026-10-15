package com.pronavicola.camion.domain.model

data class SimulationConfig(
    val port: Int = 1502,
    val baselineTemperature1: Double = 14.5,
    val baselineTemperature2: Double = 15.0,
    val baselineHumidity: Double = 60.0,
    val baselineVoltage: Double = 24.2,
    val oscillationAmplitude: Double = 1.2,
    val gateOpen: Boolean = false,
    val failureEnergy: Boolean = false,
    val failurePulverizador: Boolean = false,
    val failureDamper: Boolean = false,
    val failureCalefaccion: Boolean = false,
    val tickDelayMs: Long = 1000L,
)

data class SimulationState(
    val isRunning: Boolean = false,
    val port: Int = 1502,
    val clientCount: Int = 0,
    val currentT1: Double = 14.5,
    val currentT2: Double = 15.0,
    val currentHum: Double = 60.0,
    val currentVoltage: Double = 24.2,
    val config: SimulationConfig = SimulationConfig(),
)
