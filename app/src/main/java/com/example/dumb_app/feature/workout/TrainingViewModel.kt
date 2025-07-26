package com.example.dumb_app.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dumb_app.core.repository.TrainingRepository
import com.example.dumb_app.core.util.TrainingSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 负责训练相关的业务：
 *  - 标记计划完成
 *  - （后续）创建/更新/删除计划
 *  - （后续）写训练记录
 */
class TrainingViewModel(
    private val repo: TrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    /** 点击“完成训练”后调用，标记当前计划为 complete=true */
    fun markPlanComplete() {
        // 取出之前保存在 TrainingSession 里的 sessionId
        val sid = TrainingSession.sessionId
        if (sid == null) {
            _uiState.value = UiState.Error("未找到当前训练计划 ID")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                repo.completePlan(sid, true)
                _uiState.value = UiState.Success("标记完成成功")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("标记完成失败：${e.message}")
            }
        }
    }

    // TODO: 后续添加：fetchDayPlans(), createDayPlan(), updateDayPlan(), deleteDayPlan(), writeLogRecord() 等

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val msg: String) : UiState()
        data class Error(val msg: String) : UiState()
    }
}
