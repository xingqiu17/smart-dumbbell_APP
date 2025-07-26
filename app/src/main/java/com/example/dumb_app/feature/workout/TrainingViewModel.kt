package com.example.dumb_app.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dumb_app.core.repository.TrainingRepository
import com.example.dumb_app.core.repository.LogRepository
import com.example.dumb_app.core.model.Log.LogDayCreateReq
import com.example.dumb_app.core.network.NetworkModule
import com.example.dumb_app.core.util.TrainingSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 负责训练相关的业务：
 *  - 标记计划完成
 *  - 写训练记录（session + items + works）
 */
class TrainingViewModel(
    private val repo: TrainingRepository,
    // 为了不改 UI 的构造调用，这里给 LogRepository 一个带默认值的参数
    private val logRepo: LogRepository = LogRepository(NetworkModule.apiService)
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    /** 点击“完成训练”后调用，标记当前计划为 complete=true */
    fun markPlanComplete() {
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

    /** 一次性写入训练记录（session + items + works）。不会覆盖同日旧记录。 */
    fun saveLog(req: LogDayCreateReq) {
        viewModelScope.launch {
            // 不覆盖 markPlanComplete 的状态，按你需求也可以复用 Loading
            _uiState.value = UiState.Loading
            try {
                val dto = logRepo.createDayRecord(req)
                val rid = dto.session.recordId ?: -1
                _uiState.value = UiState.Success("训练记录已保存（#${rid}）")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("保存训练记录失败：${e.message}")
            }
        }
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val msg: String) : UiState()
        data class Error(val msg: String) : UiState()
    }
}
