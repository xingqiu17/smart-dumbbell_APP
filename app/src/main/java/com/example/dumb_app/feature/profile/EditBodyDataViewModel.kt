// app/src/main/java/com/example/dumb_app/feature/profile/EditBodyDataViewModel.kt
package com.example.dumb_app.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dumb_app.core.repository.AuthRepository
import com.example.dumb_app.core.util.ServiceLocator
import com.example.dumb_app.core.util.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface BodyUiState {
    object Idle    : BodyUiState
    object Loading : BodyUiState
    object Success : BodyUiState
    data class Error(val msg: String) : BodyUiState
}

class EditBodyDataViewModel(
    // 直接从 ServiceLocator 拿到 authRepository
    private val repo: AuthRepository = ServiceLocator.authRepository
) : ViewModel() {

    private val _ui = MutableStateFlow<BodyUiState>(BodyUiState.Idle)
    val uiState: StateFlow<BodyUiState> = _ui

    /**
     * @param birthdayStr ISO 日期字符串 "yyyy-MM-dd"
     */
    fun submit(
        birthdayStr: String,
        heightCm: Float,
        weightKg: Float,
        genderCode: Int,
        handle: SavedStateHandle
    ) {
        viewModelScope.launch {
            _ui.value = BodyUiState.Loading
            runCatching {
                repo.updateBodyData(birthdayStr, heightCm, weightKg, genderCode)
            }.onSuccess { updated ->
                // 刷新全局会话
                UserSession.update(updated)
                // 回写给 ProfileScreen
                handle["birthDate"] = updated.birthday  // UserDto.birthday 是 String
                handle["height"]    = updated.height.toInt()
                handle["weight"]    = updated.weight.toInt()
                handle["gender"]    = if (updated.gender == 1) "女" else "男"
                _ui.value = BodyUiState.Success
            }.onFailure {
                _ui.value = BodyUiState.Error(it.message ?: "更新失败")
            }
        }
    }
}
