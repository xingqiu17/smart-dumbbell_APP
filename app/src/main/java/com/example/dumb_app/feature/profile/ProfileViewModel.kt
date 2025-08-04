// app/src/main/java/com/example/dumb_app/feature/profile/ProfileViewModel.kt
package com.example.dumb_app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dumb_app.core.model.User.UserDto
import com.example.dumb_app.core.repository.AuthRepository
import com.example.dumb_app.core.util.ServiceLocator
import com.example.dumb_app.core.util.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// 1. 定义 Profile 页面状态
sealed interface ProfileUiState {
    object Idle                         : ProfileUiState
    object Loading                      : ProfileUiState
    data class Success(val user: UserDto) : ProfileUiState
    data class Error(val msg: String)     : ProfileUiState
}

// 2. ProfileViewModel
class ProfileViewModel(
    // 和 EditBodyDataViewModel 一样，从 ServiceLocator 拿 repo
    private val repo: AuthRepository = ServiceLocator.authRepository
) : ViewModel() {

    private val _ui = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _ui

    /** 每次进入 ProfileScreen 时调用，或者下拉刷新时也可以调用 */
    fun loadProfile() {
        viewModelScope.launch {
            _ui.value = ProfileUiState.Loading
            runCatching {
                // repo.fetchProfile() 会走你刚加的 api.getUser(...) 
                repo.fetchProfile()
            }.onSuccess { updated ->
                // 更新全局缓存（如果需要，fetchProfile 内也做了一次更新）
                UserSession.update(updated)
                UserSession.updateHwWeight(updated.hwWeight)
                _ui.value = ProfileUiState.Success(updated)
            }.onFailure { err ->
                _ui.value = ProfileUiState.Error(err.message ?: "加载失败")
            }
        }
    }
}
