package com.pronavicola.camion.data.repository

import com.pronavicola.camion.data.local.dao.ParametroDao
import com.pronavicola.camion.data.modbus.ModbusClient
import com.pronavicola.camion.domain.model.ParametroUiModel
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.domain.repository.ParametrosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ParametrosRepositoryImpl(
    private val parametroDao: ParametroDao,
    private val modbusClient: ModbusClient,
) : ParametrosRepository {
    override fun observeParametros(user: SessionUser): Flow<List<ParametroUiModel>> =
        parametroDao.observeParametros(user.idVehiculo).map { rows ->
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
            editable = parametroDao.canEdit(user.idPerfil, user.idVehiculo, param.idParametro),
        )
    }

    override suspend fun updateParametro(
        user: SessionUser,
        parametro: ParametroUiModel,
        nuevoValor: Double,
    ): Result<Double> {
        if (!parametroDao.canEdit(user.idPerfil, user.idVehiculo, parametro.idParametro)) {
            return Result.failure(IllegalAccessException("Sin permiso para editar ${parametro.nombre}"))
        }
        if (nuevoValor < parametro.minimo || nuevoValor > parametro.maximo) {
            return Result.failure(IllegalArgumentException("Valor fuera de rango permitido"))
        }

        parametroDao.updateValor(user.idVehiculo, parametro.idParametro, nuevoValor)
        modbusClient.writeParameter(parametro.tag, nuevoValor)
        return Result.success(nuevoValor)
    }
}
