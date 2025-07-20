// app/feature/auth/LoginViewModel.kt
package com.example.dumb_app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dumb_app.core.util.ServiceLocator
import com.example.dumb_app.core.util.PasswordUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repo: com.example.dumb_app.core.repository.AuthRepository =
        ServiceLocator.authRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    fun login(account: String, rawPwd: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            runCatching {
                repo.login(account, rawPwd)         // repo 自己会做加盐 MD5
            }.onSuccess {
                _uiState.value = UiState.Success(it.id)
            }.onFailure {
                _uiState.value = UiState.Error("账号或密码错误")
            }
        }
    }
}
