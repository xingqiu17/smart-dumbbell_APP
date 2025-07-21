package com.example.dumb_app.feature.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dumb_app.core.model.PlanDayDto    // ← 新增
import com.example.dumb_app.core.repository.TrainingRepository
import com.example.dumb_app.core.util.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface PlanUiState {
    object Loading : PlanUiState
    object Empty   : PlanUiState
    data class Success(val sessions: List<PlanDayDto>) : PlanUiState  // ← 改为 sessions 列表
    data class Error(val msg: String) : PlanUiState
}

class RecordViewModel(
    private val repo: TrainingRepository = ServiceLocator.trainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlanUiState>(PlanUiState.Loading)
    val uiState: StateFlow<PlanUiState> = _uiState

    /** 拉取指定日期的所有训练计划会话 */
    fun loadPlans(date: String) {
        _uiState.value = PlanUiState.Loading
        viewModelScope.launch {
            runCatching { repo.getDayPlan(date) }
                .onSuccess { dto ->
                    // 将单一 dto（可能为 null）包装成列表
                    val list = dto?.let { listOf(it) } ?: emptyList()
                    _uiState.value = if (list.isEmpty()) {
                        PlanUiState.Empty
                    } else {
                        PlanUiState.Success(list)
                    }
                }
                .onFailure {
                    _uiState.value = PlanUiState.Error(it.message ?: "网络错误")
                }
        }
    }
}
