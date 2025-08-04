package com.example.dumb_app.feature.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dumb_app.core.model.Plan.PlanDayDto
import com.example.dumb_app.core.model.Log.LogDayDto
import com.example.dumb_app.core.model.Log.LogWorkDto
import com.example.dumb_app.core.repository.TrainingRepository
import com.example.dumb_app.core.repository.LogRepository
import com.example.dumb_app.core.util.ServiceLocator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

/** 新增：用于“当日全部训练记录”的 UI 状态 */
sealed interface LogListUiState {
    object Loading : LogListUiState
    object Empty   : LogListUiState
    data class Success(val records: List<LogDayDto>) : LogListUiState
    data class Error(val msg: String) : LogListUiState
}

class RecordViewModel(
    private val planRepo: TrainingRepository = ServiceLocator.trainingRepository,
    private val logRepo:  LogRepository      = ServiceLocator.logRepository
) : ViewModel() {

    // 训练计划
    private val _planState = MutableStateFlow<PlanUiState>(PlanUiState.Loading)
    val planState: StateFlow<PlanUiState> = _planState

    // “单条”训练记录（保留，避免影响其它页面）
    private val _logState = MutableStateFlow<LogUiState>(LogUiState.Loading)
    val logState: StateFlow<LogUiState> = _logState

    // “当日全部”训练记录（RecordScreen 用这个）
    private val _logListState = MutableStateFlow<LogListUiState>(LogListUiState.Loading)
    val logListState: StateFlow<LogListUiState> = _logListState

    // 详情页当前选中的记录
    private val _selectedLog = MutableStateFlow<LogDayDto?>(null)
    val selectedLog: StateFlow<LogDayDto?> = _selectedLog

    // 已训练日期（用于日历高亮）
    private val _trainingDates = MutableStateFlow<List<LocalDate>>(emptyList())
    val trainingDates: StateFlow<List<LocalDate>> = _trainingDates

    // 每个 groupId 的 works 明细缓存
    private val _worksMap = MutableStateFlow<Map<Int, List<LogWorkDto>>>(emptyMap())
    val worksMap: StateFlow<Map<Int, List<LogWorkDto>>> = _worksMap

    fun selectLog(log: LogDayDto) {
        _selectedLog.value = log
        _worksMap.value = emptyMap()
    }

    fun clearSelectedLog() {
        _selectedLog.value = null
        _worksMap.value = emptyMap()
    }

    /** 拉取指定日期的所有训练计划会话 */
    fun loadPlans(date: String) {
        _planState.value = PlanUiState.Loading
        viewModelScope.launch {
            runCatching { planRepo.getDayPlans(date) }
                .onSuccess { list ->
                    _planState.value = if (list.isEmpty()) PlanUiState.Empty
                    else PlanUiState.Success(list)
                }
                .onFailure {
                    _planState.value = PlanUiState.Error(it.message ?: "网络错误")
                }
        }
    }

    /**（保留）拉“当天最新一条” */
    fun loadLogs(date: String) {
        _logState.value = LogUiState.Loading
        viewModelScope.launch {
            val dto = runCatching { logRepo.getDayRecords(date) }.getOrNull()
            _logState.value = if (dto == null || dto.session == null) LogUiState.Empty
            else LogUiState.Success(dto)
        }
    }

    /** 新增：拉取“当日全部训练记录”列表 */
    fun loadLogsAll(date: String) {
        _logListState.value = LogListUiState.Loading
        viewModelScope.launch {
            val list = runCatching { logRepo.getDayRecordsAll(date) }.getOrElse { emptyList() }
            // 按 recordId 升序（或你想要的顺序）
            val sorted = list.sortedBy { it.session.recordId ?: 0 }
            _logListState.value = if (sorted.isEmpty()) LogListUiState.Empty
            else LogListUiState.Success(sorted)
        }
    }

    /** 批量判断哪些日期有训练记录（用“列表接口”，有任意一条就算训练日） */
    fun loadTrainingDates(dates: List<LocalDate>) {
        viewModelScope.launch {
            val trainedDates = dates.mapNotNull { d ->
                runCatching { logRepo.getDayRecordsAll(d.toString()) }
                    .getOrElse { emptyList() }
                    .takeIf { it.isNotEmpty() }
                    ?.let { d }
            }
            _trainingDates.value = trainedDates
        }
    }

    /** 批量加载 works，并合并进缓存（已加载过的不再请求） */
    fun loadWorksFor(groupIds: List<Int>) {
        viewModelScope.launch {
            val existing = _worksMap.value
            val need = groupIds.filter { it !in existing }.distinct()
            if (need.isEmpty()) return@launch
            val fetched = need.map { gid ->
                async { gid to runCatching { logRepo.listWorksByGroup(gid) }.getOrElse { emptyList() } }
            }.awaitAll().toMap()
            _worksMap.value = existing + fetched
        }
    }
}
