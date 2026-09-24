package com.pronavicola.camion.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronavicola.camion.data.local.entity.UsuarioEntity
import com.pronavicola.camion.domain.model.SessionUser
import com.pronavicola.camion.domain.repository.AuthResult
import com.pronavicola.camion.domain.usecase.auth.ChangePasswordUseCase
import com.pronavicola.camion.domain.usecase.auth.CheckAccessPermissionUseCase
import com.pronavicola.camion.domain.usecase.auth.GetActiveUsersUseCase
import com.pronavicola.camion.domain.usecase.auth.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val DEFAULT_SESSION_USER = SessionUser(
    idUsuario = "OPERARIO",
    usuario = "Operario",
    idPerfil = 4,
    idVehiculo = 1,
)

data class AuthUiState(
    val currentUser: SessionUser? = DEFAULT_SESSION_USER,
    val activeUsers: List<UsuarioEntity> = emptyList(),
    val showUserDialog: Boolean = false,
    val isChangePasswordMode: Boolean = false,
    val loginPasswordInput: String = "",
    val currentPasswordInput: String = "",
    val newPasswordInput: String = "",
    val confirmPasswordInput: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class AuthViewModel(
    private val getActiveUsersUseCase: GetActiveUsersUseCase,
    private val loginUseCase: LoginUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val checkAccessPermissionUseCase: CheckAccessPermissionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val defaultUser = loginUseCase.loadDefaultUser()
            _uiState.update { it.copy(currentUser = defaultUser) }
        }

        viewModelScope.launch {
            getActiveUsersUseCase().collect { users ->
                _uiState.update { it.copy(activeUsers = users) }
            }
        }
    }

    fun openUserDialog() {
        _uiState.update {
            it.copy(
                showUserDialog = true,
                isChangePasswordMode = false,
                loginPasswordInput = "",
                currentPasswordInput = "",
                newPasswordInput = "",
                confirmPasswordInput = "",
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun closeUserDialog() {
        _uiState.update { it.copy(showUserDialog = false) }
    }

    fun setPasswordMode(isChangeMode: Boolean) {
        _uiState.update {
            it.copy(
                isChangePasswordMode = isChangeMode,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun onLoginPasswordChange(value: String) {
        _uiState.update { it.copy(loginPasswordInput = value, errorMessage = null) }
    }

    fun onCurrentPasswordChange(value: String) {
        _uiState.update { it.copy(currentPasswordInput = value, errorMessage = null) }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPasswordInput = value, errorMessage = null) }
    }

    fun switchUser(targetUserId: String) {
        val current = _uiState.value.currentUser ?: return
        val password = _uiState.value.loginPasswordInput

        viewModelScope.launch {
            when (val result = loginUseCase(current, targetUserId, password)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            currentUser = result.user,
                            showUserDialog = false,
                            loginPasswordInput = "",
                            errorMessage = null,
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun changePassword() {
        val current = _uiState.value.currentUser ?: return
        val currentPass = _uiState.value.currentPasswordInput
        val newPass = _uiState.value.newPasswordInput
        val confirmPass = _uiState.value.confirmPasswordInput

        if (newPass != confirmPass) {
            _uiState.update { it.copy(errorMessage = "Las contraseñas no coinciden") }
            return
        }

        viewModelScope.launch {
            when (val result = changePasswordUseCase(current, currentPass, newPass)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            currentUser = result.user,
                            isChangePasswordMode = false,
                            currentPasswordInput = "",
                            newPasswordInput = "",
                            confirmPasswordInput = "",
                            successMessage = "Contraseña actualizada exitosamente",
                            errorMessage = null,
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun canAccessScreen(requiredAccessLevel: Int): Boolean {
        val user = _uiState.value.currentUser ?: return false
        return checkAccessPermissionUseCase(user, requiredAccessLevel)
    }
}
