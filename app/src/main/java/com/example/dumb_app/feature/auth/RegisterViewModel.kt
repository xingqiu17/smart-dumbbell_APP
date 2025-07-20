package com.example.dumb_app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dumb_app.core.util.ServiceLocator
import com.example.dumb_app.core.util.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val userId: Int) : UiState
    data class Error(val msg: String) : UiState
}

class RegisterViewModel(
    private val repo: com.example.dumb_app.core.repository.AuthRepository =
        ServiceLocator.authRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    fun register(account: String, password: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            runCatching { repo.register(account, password) }
                .onSuccess {
                    _uiState.value = UiState.Success(it.id)
                    UserSession.update(it)}
                .onFailure { _uiState.value = UiState.Error(it.message ?: "注册失败") }
        }
    }
}
