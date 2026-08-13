package com.pronavicola.camion

import android.app.Application
import com.pronavicola.camion.data.local.FurgonesDatabase
import com.pronavicola.camion.data.modbus.ModbusClient
import com.pronavicola.camion.data.modbus.SimulatedModbusClient
import com.pronavicola.camion.data.repository.AuthRepository
import com.pronavicola.camion.data.repository.ParametrosRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class CamionApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob())

    val database: FurgonesDatabase by lazy { FurgonesDatabase.create(this) }
    val modbusClient: ModbusClient by lazy { SimulatedModbusClient(appScope) }

    val authRepository: AuthRepository by lazy {
        AuthRepository(database.usuarioDao())
    }

    val parametrosRepository: ParametrosRepository by lazy {
        ParametrosRepository(database.parametroDao(), modbusClient)
    }
}
