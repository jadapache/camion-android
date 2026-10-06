package com.pronavicola.camion.domain.usecase.alarmas

import com.pronavicola.camion.data.local.entity.AlarmaEntity
import com.pronavicola.camion.data.local.entity.AlarmaRegistroEntity
import com.pronavicola.camion.domain.repository.AlarmaRegistroRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AlarmaRegistroUiModel(
    val idAlarma: Int,
    val idVehiculo: Int,
    val idViaje: Int,
    val alarmaDescripcion: String,
    val horaInicio: String,
    val horaFin: String,
    val estado: String?,
)

class ObserveAlarmasRegistrosUseCase(
    private val alarmaRegistroRepository: AlarmaRegistroRepository
) {
    suspend operator fun invoke(idVehiculo: Int): Flow<List<AlarmaRegistroUiModel>> {
        val alarmas = alarmaRegistroRepository.getAlarmas().associateBy { it.idAlarma }
        return alarmaRegistroRepository.observeAlarmasRegistros(idVehiculo).map { registros ->
            registros.map { registro ->
                val alarma = registro.alarmasIdAlarma?.let { alarmas[it] } ?: alarmas[registro.idAlarma]
                AlarmaRegistroUiModel(
                    idAlarma = registro.idAlarma,
                    idVehiculo = registro.idVehiculo,
                    idViaje = registro.idViaje,
                    alarmaDescripcion = alarma?.alarma ?: "Alarma Desconocida (${registro.idAlarma})",
                    horaInicio = registro.horaInicio,
                    horaFin = registro.horaFin,
                    estado = registro.estado,
                )
            }
        }
    }
}
