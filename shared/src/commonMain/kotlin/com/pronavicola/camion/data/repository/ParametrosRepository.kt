package com.pronavicola.camion.data.repository

import com.pronavicola.camion.data.local.dao.ParametroDao
import com.pronavicola.camion.data.local.dao.VehiculoDao
import com.pronavicola.camion.data.modbus.ModbusClient
import com.pronavicola.camion.domain.model.ParametroUiModel
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.domain.repository.ParametrosRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class ParametrosRepositoryImpl(
    private val parametroDao: ParametroDao,
    private val vehiculoDao: VehiculoDao,
    private val modbusClient: ModbusClient,
) : ParametrosRepository {
    override fun observeParametros(user: SessionUser): Flow<List<ParametroUiModel>> =
        vehiculoDao.observePredeterminado().flatMapLatest { activeVehiculo ->
            val vehiculoId = activeVehiculo?.idVehiculo ?: 1
            parametroDao.observeParametros(vehiculoId).flatMapLatest { list ->
                if (list.isEmpty() && vehiculoId != 1) {
                    parametroDao.observeParametros(1)
                } else {
                    kotlinx.coroutines.flow.flowOf(list)
                }
            }
        }.map { rows ->
            rows.map { row ->
                ParametroUiModel(
                    idParametro = row.idParametro,
                    nombre = row.parametro,
                    valor = row.valor,
                    unidad = row.unidad,
                    minimo = row.minimo,
                    maximo = row.maximo,
                    esDecimal = row.idTipo == "TIPONUME.DEC",
                    editable = false,
                    tag = row.tag,
                )
            }
        }

    override suspend fun enrichWithPermissions(
        parametros: List<ParametroUiModel>,
        user: SessionUser,
    ): List<ParametroUiModel> = parametros.map { param ->
        param.copy(
            editable = parametroDao.canEdit(user.idPerfil, param.idParametro),
        )
    }

    override suspend fun updateParametro(
        user: SessionUser,
        parametro: ParametroUiModel,
        nuevoValor: Double,
    ): Result<Double> {
        val vehiculoId = vehiculoDao.getPredeterminado()?.idVehiculo ?: 1
        val targetVehiculoId = if (parametroDao.getByVehiculo(vehiculoId).isNotEmpty()) vehiculoId else 1
        if (!parametroDao.canEdit(user.idPerfil, parametro.idParametro)) {
            return Result.failure(IllegalAccessException("Sin permiso para editar ${parametro.nombre}"))
        }
        if (nuevoValor < parametro.minimo || nuevoValor > parametro.maximo) {
            return Result.failure(IllegalArgumentException("Valor fuera de rango permitido"))
        }

        parametroDao.updateValor(targetVehiculoId, parametro.idParametro, nuevoValor)
        modbusClient.writeParameter(parametro.tag, nuevoValor)
        return Result.success(nuevoValor)
    }
}
