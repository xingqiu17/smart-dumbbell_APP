package com.example.dumb_app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dumb_app.core.repository.AuthRepository
import com.example.dumb_app.core.util.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface TrainUiState {
    object Idle    : TrainUiState
    object Loading : TrainUiState
    object Success : TrainUiState
    data class Error(val msg: String) : TrainUiState
}

class EditTrainDataViewModel(
    private val repo: AuthRepository = ServiceLocator.authRepository
) : ViewModel() {

    private val _ui = MutableStateFlow<TrainUiState>(TrainUiState.Idle)
    val uiState: StateFlow<TrainUiState> = _ui

    /** 直接传业务字段，uid 内部自动获取 */
    fun submit(aim: Int, weight: Float) {
        viewModelScope.launch {
            _ui.value = TrainUiState.Loading
            runCatching { repo.updateTrainData(aim, weight) }
                .onSuccess { _ui.value = TrainUiState.Success }
                .onFailure { _ui.value = TrainUiState.Error(it.message ?: "更新失败") }
        }
    }
}
