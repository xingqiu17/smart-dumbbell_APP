package com.example.dumb_app.feature.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dumb_app.core.model.Plan.PlanDayDto
import com.example.dumb_app.core.model.Log.LogDayDto         // ← 新增
import com.example.dumb_app.core.repository.TrainingRepository
import com.example.dumb_app.core.repository.LogRepository   // ← 新增
import com.example.dumb_app.core.util.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface PlanUiState {
    object Loading : PlanUiState
    object Empty   : PlanUiState
    data class Success(val sessions: List<PlanDayDto>) : PlanUiState
    data class Error(val msg: String) : PlanUiState
}

sealed interface LogUiState {
    object Loading : LogUiState
    object Empty   : LogUiState
    data class Success(val record: LogDayDto) : LogUiState
    data class Error(val msg: String) : LogUiState
}

class RecordViewModel(
    private val planRepo: TrainingRepository = ServiceLocator.trainingRepository,
    private val logRepo:  LogRepository      = ServiceLocator.logRepository    // ← 新增
) : ViewModel() {

    // 训练计划状态
    private val _planState = MutableStateFlow<PlanUiState>(PlanUiState.Loading)
    val planState: StateFlow<PlanUiState> = _planState

    // 训练记录状态
    private val _logState = MutableStateFlow<LogUiState>(LogUiState.Loading)
    val logState: StateFlow<LogUiState> = _logState


    private val _selectedLog = MutableStateFlow<LogDayDto?>(null)
    val selectedLog: StateFlow<LogDayDto?> = _selectedLog

    private val _trainingDates = MutableStateFlow<List<LocalDate>>(emptyList())
    val trainingDates: StateFlow<List<LocalDate>> = _trainingDates

    fun selectLog(log: LogDayDto) {
        _selectedLog.value = log
    }
    fun clearSelectedLog() {
        _selectedLog.value = null
    }

    /** 拉取指定日期的所有训练计划会话 */
    fun loadPlans(date: String) {
        _planState.value = PlanUiState.Loading
        viewModelScope.launch {
            runCatching { planRepo.getDayPlan(date) }
                .onSuccess { dto ->
                    val list = dto?.let { listOf(it) } ?: emptyList()
                    _planState.value = if (list.isEmpty()) PlanUiState.Empty
                    else PlanUiState.Success(list)
                }
                .onFailure {
                    _planState.value = PlanUiState.Error(it.message ?: "网络错误")
                }
        }
    }

    /** 拉取指定日期的所有训练记录 */
    fun loadLogs(date: String) {
        _logState.value = LogUiState.Loading
        viewModelScope.launch {
            runCatching { logRepo.getDayRecords(date) }
                .onSuccess { dto ->
                    if (dto == null || dto.session == null) {
                        // session 为 null，说明后端没记录，走 Empty 分支
                        _logState.value = LogUiState.Empty
                    } else {
                        // session 不为空，才是真正的 Success
                        _logState.value = LogUiState.Success(dto)
                    }
                }
                .onFailure {
                    _logState.value = LogUiState.Error(it.message ?: "网络错误")
                }
        }
    }

    fun loadTrainingDates(dates: List<LocalDate>) {
        viewModelScope.launch {
            val trained = dates.mapNotNull { date ->
                runCatching { logRepo.getDayRecords(date.toString()) }
                    .getOrNull()
                    // 如果有 session，不为空就保留这一天
                    ?.takeIf { it.session != null }?.session?.date
                    ?.let { LocalDate.parse(it) }
            }
            _trainingDates.value = trained
        }
    }
}
