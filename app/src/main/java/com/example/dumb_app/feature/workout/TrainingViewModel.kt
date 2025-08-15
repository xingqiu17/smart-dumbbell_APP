package com.example.dumb_app.feature.workout

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dumb_app.core.model.Log.LogDayCreateReq
import com.example.dumb_app.core.model.Log.LogItemCreateReq
import com.example.dumb_app.core.model.Log.LogWorkCreateReq
import com.example.dumb_app.core.network.NetworkModule
import com.example.dumb_app.core.repository.LogRepository
import com.example.dumb_app.core.repository.TrainingRepository
import com.example.dumb_app.core.util.TrainingSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 每个动作明细（组内的第几次与得分 + exLabel） */
data class WorkRec(val acOrder: Int, val score: Int, val exLabel: Int?)

/** 一组（set）的累计结构：与后端 items 一一对应 */
data class PendingItem(
    val type: Int,
    val tOrder: Int,
    val tWeight: Float,
    val num: Int,
    val works: MutableList<WorkRec> = mutableListOf(),
    /** 该组目前已处理到的最大 rep，用于去重/补齐 */
    var lastRep: Int = 0
) {
    val avgScore: Int
        get() = if (works.isEmpty()) 0 else works.map { it.score }.average().roundToInt()
}

/**
 * 负责训练相关的业务：
 *  - 维护各组的累计明细（跨导航不丢）
 *  - 标记计划完成
 *  - 写训练记录（session + items + works）
 */
class TrainingViewModel(
    private val repo: TrainingRepository,
    private val logRepo: LogRepository = LogRepository(NetworkModule.apiService)
) : ViewModel() {

    companion object {
        private const val TAG_TVM = "TrainingViewModel"
    }

    /** UI 进度/错误状态 */
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    /** 全部组次的累计明细（与 TrainingSession.items 对齐） */
    private val _pendingItems = MutableStateFlow<List<PendingItem>>(emptyList())
    val pendingItems: StateFlow<List<PendingItem>> = _pendingItems

    /** 仅在首次进入训练页时调用一次，基于 TrainingSession 初始化每组 */
    fun initFromSession(force: Boolean = false) {
        if (!force && _pendingItems.value.isNotEmpty()) return
        val list = TrainingSession.items.mapIndexed { idx, it ->
            val order = it.tOrder ?: (idx + 1)
            PendingItem(
                type = it.type,
                tOrder = order,
                tWeight = it.tWeight,
                num = it.number
            )
        }
        _pendingItems.value = list
    }

    /**
     * 处理硬件上报：只把数据写入“当前组”
     * - expectedType 不匹配则直接丢弃
     * - 去重：rep <= lastRep 丢弃
     * - 补齐：若首次就是 3，则补 1、2 为 0 分占位（exLabel=null）
     * - 关键：采用 **不可变发布**（复制 works、复制 PendingItem、复制外层 List）
     */
    fun applyExerciseData(
        setIndex: Int,
        expectedType: Int,
        rep: Int,
        score: Double?,
        exLabel: Int?
    ) {
        val oldList = _pendingItems.value
        val cur = oldList.getOrNull(setIndex) ?: return
        if (cur.type != expectedType) return
        if (rep <= 0) return

        // 去重：忽略回放/重复
        if (rep <= cur.lastRep) return

        // 组内上限
        val k = rep.coerceAtMost(cur.num)

        // 拷贝一个新的 works，再在副本上修改
        val newWorks = cur.works.toMutableList()

        // 补齐缺失（例如第一条就是 3）
        while (newWorks.size < k - 1) {
            newWorks.add(WorkRec(acOrder = newWorks.size + 1, score = 0, exLabel = null))
        }

        val s = (score?.roundToInt() ?: 0)
        val rec = WorkRec(acOrder = k, score = s, exLabel = exLabel)
        if (newWorks.size >= k) {
            newWorks[k - 1] = rec
        } else {
            newWorks.add(rec)
        }

        // 复制 PendingItem，并写回新的外层 List
        val newPi = cur.copy(works = newWorks, lastRep = k)
        val newList = oldList.toMutableList().apply { this[setIndex] = newPi }.toList()
        _pendingItems.value = newList
    }

    /** （可选）保存前把每组的未完成次数补 0，确保 works.size == num（不可变发布） */
    fun fillMissingZeros() {
        val newList = _pendingItems.value.map { pi ->
            val ws = pi.works.toMutableList()
            while (ws.size < pi.num) {
                ws.add(WorkRec(acOrder = ws.size + 1, score = 0, exLabel = null))
            }
            pi.copy(works = ws, lastRep = ws.size)
        }
        _pendingItems.value = newList
    }

    /** 保存当前累计数据，未完成部分补 0 后写入日志 */
    fun savePartialTraining(userId: Int) {
        Log.d(TAG_TVM, "savePartialTraining: userId=$userId")
        fillMissingZeros()
        val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val req = buildLogDayCreateReq(userId = userId, date = dateStr)
        Log.d(TAG_TVM, "built req: $req")
        saveLog(req)
    }

    /** 生成 LogDayCreateReq（用于 Screen 弹窗确认后提交） */
    fun buildLogDayCreateReq(
        userId: Int,
        date: String = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
    ): LogDayCreateReq {
        val itemsReq = _pendingItems.value.map { pi ->
            LogItemCreateReq(
                type     = pi.type,
                tOrder   = pi.tOrder,
                tWeight  = pi.tWeight,
                num      = pi.num,
                avgScore = pi.avgScore,
                works    = pi.works.map { w -> LogWorkCreateReq(w.acOrder, w.score) }
            )
        }
        return LogDayCreateReq(userId = userId, date = date, items = itemsReq)
    }

    /** 一次性写入训练记录（session + items + works）。不会覆盖同日旧记录。 */
    fun saveLog(req: LogDayCreateReq) {
        viewModelScope.launch {
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

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val msg: String) : UiState()
        data class Error(val msg: String) : UiState()
    }
}
