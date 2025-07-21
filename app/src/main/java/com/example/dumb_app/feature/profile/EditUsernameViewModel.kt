package com.example.dumb_app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.dumb_app.core.repository.AuthRepository
import com.example.dumb_app.core.util.ServiceLocator
import com.example.dumb_app.core.util.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface NameUiState {
    object Idle    : NameUiState
    object Loading : NameUiState
    object Success : NameUiState
    data class Error(val msg: String) : NameUiState
}

class EditUsernameViewModel(
    private val repo: AuthRepository = ServiceLocator.authRepository
) : ViewModel() {

    private val _ui = MutableStateFlow<NameUiState>(NameUiState.Idle)
    val uiState: StateFlow<NameUiState> = _ui

    /**
     * 向后端提交新用户名，并把结果回写到 handle（给 ProfileScreen 刷 UI）
     */
    fun submit(newName: String, handle: SavedStateHandle) {
        viewModelScope.launch {
            _ui.value = NameUiState.Loading
            runCatching { repo.updateName(newName) }
                .onSuccess { updated ->
                    // 1. 已在 repo 内刷新了 UserSession
                    // 2. 回传给上一页
                    handle["username"] = updated.name
                    _ui.value = NameUiState.Success
                }
                .onFailure { _ui.value = NameUiState.Error(it.message ?: "更新失败") }
        }
    }
}
