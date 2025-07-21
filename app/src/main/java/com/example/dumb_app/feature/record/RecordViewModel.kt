package com.example.dumb_app.feature.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dumb_app.core.model.PlanItemDto
import com.example.dumb_app.core.model.PlanSessionDto
import com.example.dumb_app.core.repository.TrainingRepository
import com.example.dumb_app.core.util.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** UI 状态 */
sealed interface PlanUiState {
    object Loading : PlanUiState
    object Empty   : PlanUiState                 // 当天没有计划
    data class Success(
        val session: PlanSessionDto,            // 头信息：日期 / 完成状态 等
        val items:   List<PlanItemDto>          // 明细：已按 tOrder 排序
    ) : PlanUiState
    data class Error(val msg: String) : PlanUiState
}

class RecordViewModel(
    private val repo: TrainingRepository = ServiceLocator.trainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlanUiState>(PlanUiState.Loading)
    val uiState: StateFlow<PlanUiState> = _uiState

    /** 拉取指定日期（yyyy-MM-dd）的训练计划 */
    fun loadPlans(date: String) {
        _uiState.value = PlanUiState.Loading
        viewModelScope.launch {
            runCatching { repo.getDayPlan(date) }
                .onSuccess { dto ->
                    _uiState.value = when {
                        dto == null          -> PlanUiState.Empty
                        dto.items.isEmpty()  -> PlanUiState.Empty
                        else                 -> PlanUiState.Success(dto.session, dto.items)
                    }
                }
                .onFailure { e ->
                    _uiState.value = PlanUiState.Error(e.message ?: "网络错误")
                }
        }
    }
}
