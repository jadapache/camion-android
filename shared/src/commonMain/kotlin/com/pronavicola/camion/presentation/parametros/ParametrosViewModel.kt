package com.pronavicola.camion.presentation.parametros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronavicola.camion.domain.model.ParametroUiModel
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.domain.usecase.parametros.ObserveParametrosUseCase
import com.pronavicola.camion.domain.usecase.parametros.UpdateParametroUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParametrosUiState(
    val parametros: List<ParametroUiModel> = emptyList(),
    val searchQuery: String = "",
    val editingParametro: ParametroUiModel? = null,
    val editValue: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val filteredParametros: List<ParametroUiModel>
        get() = if (searchQuery.isBlank()) {
            parametros
        } else {
            parametros.filter {
                it.nombre.contains(searchQuery, ignoreCase = true) ||
                        it.tag.contains(searchQuery, ignoreCase = true)
            }
        }
}

class ParametrosViewModel(
    private val observeParametrosUseCase: ObserveParametrosUseCase,
    private val updateParametroUseCase: UpdateParametroUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParametrosUiState())
    val uiState: StateFlow<ParametrosUiState> = _uiState.asStateFlow()

    fun loadParametros(user: SessionUser) {
        viewModelScope.launch {
            observeParametrosUseCase(user).collect { list ->
                _uiState.update { it.copy(parametros = list) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun startEditing(parametro: ParametroUiModel) {
        _uiState.update {
            it.copy(
                editingParametro = parametro,
                editValue = if (parametro.esDecimal) parametro.valor.toString() else parametro.valor.toLong().toString(),
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun cancelEditing() {
        _uiState.update {
            it.copy(
                editingParametro = null,
                editValue = "",
                errorMessage = null,
            )
        }
    }

    fun onEditValueChanged(newValue: String) {
        _uiState.update { it.copy(editValue = newValue, errorMessage = null) }
    }

    fun saveParameter(user: SessionUser) {
        val target = _uiState.value.editingParametro ?: return
        val numericVal = _uiState.value.editValue.toDoubleOrNull()
        if (numericVal == null) {
            _uiState.update { it.copy(errorMessage = "Ingrese un valor numérico válido") }
            return
        }

        if (numericVal < target.minimo || numericVal > target.maximo) {
            _uiState.update { it.copy(errorMessage = "El valor debe estar entre ${target.minimo} y ${target.maximo}") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = updateParametroUseCase(user, target, numericVal)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        editingParametro = null,
                        editValue = "",
                        successMessage = "Parámetro '${target.nombre}' actualizado correctamente a $numericVal",
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Error al actualizar parámetro",
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
