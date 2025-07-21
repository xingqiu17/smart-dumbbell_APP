package com.example.dumb_app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dumb_app.core.repository.AuthRepository
import com.example.dumb_app.core.util.ServiceLocator
import com.example.dumb_app.core.util.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface TrainUiState {
    object Idle    : TrainUiState
    object Loading : TrainUiState
    object Success : TrainUiState          // 不再携带数据，页面本地已有
    data class Error(val msg: String) : TrainUiState
}

class EditTrainDataViewModel(
    private val repo: AuthRepository = ServiceLocator.authRepository
) : ViewModel() {

    private val _ui = MutableStateFlow<TrainUiState>(TrainUiState.Idle)
    val uiState: StateFlow<TrainUiState> = _ui

    /** 只负责向后端提交；成功后刷新 UserSession，并通知 UI */
    fun submit(aim: Int, weight: Float) {
        viewModelScope.launch {
            _ui.value = TrainUiState.Loading
            runCatching { repo.updateTrainData(aim, weight) }
                .onSuccess { updated ->
                    UserSession.update(updated)      // 刷新全局会话
                    _ui.value = TrainUiState.Success // 通知界面保存成功
                }
                .onFailure { err ->
                    _ui.value = TrainUiState.Error(err.message ?: "更新失败")
                }
        }
    }
}
