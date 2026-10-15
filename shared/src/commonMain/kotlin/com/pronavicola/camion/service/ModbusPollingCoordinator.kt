package com.pronavicola.camion.service

import com.pronavicola.camion.data.local.entity.VariableRegistroEntity
import com.pronavicola.camion.data.modbus.ModbusClient
import com.pronavicola.camion.data.mqtt.MqttClientManager
import com.pronavicola.camion.data.mqtt.TelemetryMqttCoordinator
import com.pronavicola.camion.domain.repository.AppConfigRepository
import com.pronavicola.camion.domain.repository.RegistrosRepository
import com.pronavicola.camion.data.local.dao.AlarmaDao
import com.pronavicola.camion.data.local.dao.AlarmaRegistroDao
import com.pronavicola.camion.data.local.entity.AlarmaEntity
import com.pronavicola.camion.data.local.entity.AlarmaRegistroEntity
import com.pronavicola.camion.domain.model.modbus.VariablesPLC
import com.pronavicola.camion.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Multiplatform continuous coordinator for polling Modbus PLC,
 * persisting telemetry records directly to SQLite (VariableRegistroEntity),
 * and forwarding them to the MQTT telemetry publisher.
 *
 * Runs seamlessly in background services, keeping data recording active
 * even when the screen is turned off or the app is minimized.
 */
class ModbusPollingCoordinator(
    private val modbusClient: ModbusClient,
    private val registrosRepository: RegistrosRepository,
    private val appConfigRepository: AppConfigRepository,
    private val telemetryMqttCoordinator: TelemetryMqttCoordinator,
    private val mqttClientManager: MqttClientManager,
    private val alarmaDao: AlarmaDao,
    private val alarmaRegistroDao: AlarmaRegistroDao,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val gpsLocationTracker: GpsLocationTracker? = null,
) {
    private val tag = "ModbusPollingCoordinator"
    private var pollingJob: Job? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        pollingJob?.cancel()

        pollingJob = scope.launch(ioDispatcher) {
            Log.i(tag, "Starting continuous Modbus polling and SQLite persistence loop...")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            // Reactive collector for instant alarm change detection and SQLite recording
            launch {
                modbusClient.alarmasActivas.collect { activeNames ->
                    evaluateAlarms(dateFormat.format(Date()), activeNames)
                }
            }

            while (isActive) {
                try {
                    val isConnected = modbusClient.isConnected.value
                    val t1 = modbusClient.temperatura1.value
                    val t2 = modbusClient.temperatura2.value
                    val hum = modbusClient.humedad.value
                    val tension = modbusClient.tension.value
                    val procesoActivo = modbusClient.procesoActivo.value

                    // Condition: When Modbus is disconnected, do not save dummy zeros or stale telemetry
                    if (isConnected && (t1 != null || t2 != null || hum != null)) {
                        val deviceId = appConfigRepository.getDeviceId() ?: "ABC123"
                        val isMqttConnected = mqttClientManager.isConnected.value

                        val registro = VariableRegistroEntity(
                            idVehiculo = 1,
                            fecha = dateFormat.format(Date()),
                            temperatura1 = t1 ?: 0.0,
                            temperatura2 = t2 ?: 0.0,
                            humedad = hum ?: 0.0,
                            tension = tension ?: 0.0,
                        )

                        // 1. Persist to local SQLite database
                        registrosRepository.saveRegistro(registro)
                        Log.d(tag, "Saved telemetry record to SQLite: T1=$t1, T2=$t2, Hum=$hum, Tension=$tension")

                        // 2. Transmit via MQTT outbox coordinator with current GPS location
                        val loc = gpsLocationTracker?.currentLocation?.value
                        telemetryMqttCoordinator.enqueueVariableRegistro(registro, loc)
                    } else if (!isConnected) {
                        Log.v(tag, "Modbus disconnected: skipping SQLite persistence to avoid recording zero/stale values.")
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Error during Modbus polling/persistence tick: ${e.message}")
                }

                if (modbusClient.isConnected.value) {
                    evaluateAlarms(dateFormat.format(Date()))
                }

                // DB persistence & telemetry enqueue interval from app config (default 30 seconds)
                val configInterval = appConfigRepository.getPublishInterval()?.toLongOrNull()
                val intervalSeconds = if (configInterval != null && configInterval >= 10L) configInterval else 30L
                val delayMs = intervalSeconds * 1000L
                delay(delayMs)
            }
        }
    }

    private suspend fun evaluateAlarms(
        currentTimestamp: String,
        activeNames: List<String> = modbusClient.alarmasActivas.value
    ) {
        try {
            // 1. Ensure Catalog Exists
            if (alarmaDao.getCount() == 0) {
                val catalog = VariablesPLC.Alarms.all.map {
                    AlarmaEntity(idAlarma = it.id, alarma = it.name, tag = it.tag)
                }
                alarmaDao.insertAll(catalog)
            }

            val idVehiculo = 1
            val idViaje = 1

            // 2. Fetch all defined alarms from DB
            val dbAlarmas = alarmaDao.getAll()

            // 3. Robust matching between active alarm names and DB Alarmas (IDs 1..11)
            val activeAlarmIds = mutableSetOf<Int>()
            for (active in activeNames) {
                val clean = active.trim().lowercase()
                val matched = dbAlarmas.find { dbA ->
                    val dbName = dbA.alarma?.trim()?.lowercase() ?: ""
                    val dbTag = dbA.tag?.trim()?.lowercase() ?: ""
                    dbName == clean || dbTag == clean ||
                    (clean.contains("energia") && dbName.contains("energia")) ||
                    (clean.contains("zona 1") && dbName.contains("zona 1")) ||
                    (clean.contains("zona 2") && dbName.contains("zona 2")) ||
                    (clean.contains("zona 3") && dbName.contains("zona 3")) ||
                    (clean.contains("zona 4") && dbName.contains("zona 4")) ||
                    (clean.contains("zona 5") && dbName.contains("zona 5")) ||
                    (clean.contains("zona 6") && dbName.contains("zona 6")) ||
                    ((clean.contains("humifica") || clean.contains("pulverizador")) && dbName.contains("humifica")) ||
                    (clean.contains("damper") && dbName.contains("damper")) ||
                    ((clean.contains("calefaccion") || clean.contains("calefacción")) && dbName.contains("calefaccion")) ||
                    ((clean.contains("agua") || clean.contains("tension") || clean.contains("tensión")) && dbName.contains("agua"))
                }
                if (matched != null) {
                    activeAlarmIds.add(matched.idAlarma)
                }
            }

            // 4. Fetch currently open alarms from DB (Estado = 'Activa')
            val historicalAlarmas = alarmaRegistroDao.getByVehiculoAndViaje(idVehiculo, idViaje)
            val openAlarmRecords = historicalAlarmas.filter { it.estado == "Activa" }.associateBy { it.idAlarma }

            // 5. Close alarms that are no longer active
            for (openRecord in openAlarmRecords.values) {
                if (!activeAlarmIds.contains(openRecord.idAlarma)) {
                    val closedRecord = openRecord.copy(
                        horaFin = currentTimestamp,
                        estado = "Inactiva"
                    )
                    alarmaRegistroDao.update(closedRecord)
                    Log.d(tag, "Alarma ${openRecord.idAlarma} desactivada en DB")
                }
            }

            // 6. Open new alarms
            for (alarmId in activeAlarmIds) {
                if (!openAlarmRecords.containsKey(alarmId)) {
                    val newRecord = AlarmaRegistroEntity(
                        idVehiculo = idVehiculo,
                        idAlarma = alarmId,
                        idViaje = idViaje,
                        horaInicio = currentTimestamp,
                        horaFin = "",
                        estado = "Activa",
                        alarmasIdAlarma = alarmId,
                        vehiculosIdVehiculo = idVehiculo
                    )
                    // If a record for this combination already exists (from a previous activation in the same trip), 
                    // OnConflictStrategy.REPLACE will overwrite it. This is fine per the legacy structure which keeps 1 record per trip per alarm type.
                    alarmaRegistroDao.insert(newRecord)
                    Log.d(tag, "Alarma $alarmId activada en DB")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error evaluando alarmas: ${e.message}", e)
        }
    }

    fun stop() {
        isRunning = false
        pollingJob?.cancel()
        pollingJob = null
        Log.i(tag, "Modbus polling and persistence loop stopped.")
    }
}
