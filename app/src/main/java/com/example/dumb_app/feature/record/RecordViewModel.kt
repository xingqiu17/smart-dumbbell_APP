package com.example.dumb_app.feature.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dumb_app.core.model.Plan.PlanDayDto
import com.example.dumb_app.core.model.Log.LogDayDto
import com.example.dumb_app.core.model.Log.LogWorkDto   // ← 新增
import com.example.dumb_app.core.repository.TrainingRepository
import com.example.dumb_app.core.repository.LogRepository
import com.example.dumb_app.core.util.ServiceLocator
import kotlinx.coroutines.async                         // ← 新增
import kotlinx.coroutines.awaitAll                      // ← 新增
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
    private val logRepo:  LogRepository      = ServiceLocator.logRepository
) : ViewModel() {

    // 训练计划状态
    private val _planState = MutableStateFlow<PlanUiState>(PlanUiState.Loading)
    val planState: StateFlow<PlanUiState> = _planState

    // 训练记录状态（“当天最新一条”或你选择的一条）
    private val _logState = MutableStateFlow<LogUiState>(LogUiState.Loading)
    val logState: StateFlow<LogUiState> = _logState

    // 当前在详情页展示的那条记录
    private val _selectedLog = MutableStateFlow<LogDayDto?>(null)
    val selectedLog: StateFlow<LogDayDto?> = _selectedLog

    // 已训练日期集合
    private val _trainingDates = MutableStateFlow<List<LocalDate>>(emptyList())
    val trainingDates: StateFlow<List<LocalDate>> = _trainingDates

    // 新增：每个 groupId 的 works 明细缓存
    private val _worksMap = MutableStateFlow<Map<Int, List<LogWorkDto>>>(emptyMap())
    val worksMap: StateFlow<Map<Int, List<LogWorkDto>>> = _worksMap

    fun selectLog(log: LogDayDto) {
        _selectedLog.value = log
        _worksMap.value = emptyMap() // 切换选中记录时，清空上一次的 works 缓存
    }

    fun clearSelectedLog() {
        _selectedLog.value = null
        _worksMap.value = emptyMap() // 退出详情页时同步清空
    }

    /** 拉取指定日期的所有训练计划会话 */
    fun loadPlans(date: String) {
        _planState.value = PlanUiState.Loading
        viewModelScope.launch {
            runCatching { planRepo.getDayPlans(date) }
                .onSuccess { list ->
                    _planState.value = if (list.isEmpty()) {
                        PlanUiState.Empty
                    } else {
                        PlanUiState.Success(list)
                    }
                }
                .onFailure {
                    _planState.value = PlanUiState.Error(it.message ?: "网络错误")
                }
        }
    }

    /** 拉取指定日期的“最新一条”训练记录（LogRepository 已封装取最大 recordId） */
    fun loadLogs(date: String) {
        _logState.value = LogUiState.Loading
        viewModelScope.launch {
            val dto = runCatching { logRepo.getDayRecords(date) }.getOrNull()
            _logState.value = if (dto == null || dto.session == null) {
                LogUiState.Empty
            } else {
                LogUiState.Success(dto)
            }
        }
    }

    /** 拉取一批日期里哪些当天有训练（仅作“是否有记录”的判断） */
    fun loadTrainingDates(dates: List<LocalDate>) {
        viewModelScope.launch {
            val trainedDates = dates.mapNotNull { d ->
                runCatching { logRepo.getDayRecords(d.toString()) }
                    .getOrNull()
                    ?.takeIf { it.session != null }
                    ?.session
                    ?.date
                    ?.let { LocalDate.parse(it) }
            }
            _trainingDates.value = trainedDates
        }
    }

    /** 新增：按一组 groupId 批量加载 works，并合并进缓存（幂等，已加载过的不再请求） */
    fun loadWorksFor(groupIds: List<Int>) {
        viewModelScope.launch {
            val existing = _worksMap.value
            val need = groupIds.filter { it !in existing.keys }.distinct()
            if (need.isEmpty()) return@launch

            val fetched = need.map { gid ->
                async { gid to runCatching { logRepo.listWorksByGroup(gid) }.getOrElse { emptyList() } }
            }.awaitAll().toMap()

            _worksMap.value = existing + fetched
        }
    }
}
