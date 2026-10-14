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
    val placa: String = "",
    val alarmaDescripcion: String,
    val horaInicio: String,
    val horaFin: String,
    val estado: String?,
)

class ObserveAlarmasRegistrosUseCase(
    private val alarmaRegistroRepository: AlarmaRegistroRepository
) {
    suspend operator fun invoke(idVehiculo: Int? = null): Flow<List<AlarmaRegistroUiModel>> {
        val flow = if (idVehiculo != null && idVehiculo > 0) {
            alarmaRegistroRepository.observeAlarmasViews(idVehiculo)
        } else {
            alarmaRegistroRepository.observeAllAlarmasViews()
        }
        return flow.map { views ->
            views.map { v ->
                AlarmaRegistroUiModel(
                    idAlarma = v.idAlarma,
                    idVehiculo = v.idVehiculo,
                    idViaje = v.idViaje,
                    placa = v.placa,
                    alarmaDescripcion = if (v.alarma.isNotBlank()) v.alarma else "Alarma ${v.idAlarma}",
                    horaInicio = v.horaInicio,
                    horaFin = v.horaFin,
                    estado = v.estado,
                )
            }
        }
    }
}
