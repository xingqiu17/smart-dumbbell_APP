package com.example.dumb_app.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dumb_app.core.model.Plan.PlanDayCreateDto
import com.example.dumb_app.core.model.Plan.PlanDayDto
import com.example.dumb_app.core.model.Plan.PlanItemCreateDto
import com.example.dumb_app.core.repository.TrainingRepository
import com.example.dumb_app.core.util.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/* ========= 行模型 ========= */
sealed interface PlanRow {
    data class ActionRow(
        val action: String = "",
        val quantity: String = ""
    ) : PlanRow

    data class RestRow(
        val restSeconds: String = "0"
    ) : PlanRow
}


/* ========= UIState ========= */
data class CreatePlanUiState(
    val date: LocalDate = LocalDate.now(),
    val rows: List<PlanRow> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val savedSessionId: Int? = null
)

class CreatePlanViewModel(
    private val repo: TrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePlanUiState())
    val uiState: StateFlow<CreatePlanUiState> = _uiState.asStateFlow()

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /* ---------- 加载已有计划 ---------- */
    fun loadExistingPlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val dateStr = _uiState.value.date.format(dateFmt)
            val plans: List<PlanDayDto> = repo.getDayPlans(dateStr)

            if (plans.isNotEmpty()) {
                // 取 sessionId 最大的那条
                val latest = plans.maxBy { it.session.sessionId ?: 0 }
                val sessionDate = LocalDate.parse(latest.session.date, dateFmt)

                val rowList = mutableListOf<PlanRow>()
                latest.items.forEachIndexed { idx, item ->
                    // 映射动作
                    val actName = when (item.type) {
                        1 -> "哑铃弯举"
                        2 -> "卧推"
                        else -> ""
                    }
                    rowList += PlanRow.ActionRow(
                        action   = actName,
                        quantity = item.number.toString()
                    )
                    // 除最后动作外，插入 RestRow（使用 item.rest）
                    if (idx < latest.items.lastIndex) {
                        rowList += PlanRow.RestRow(item.rest?.toString() ?: "0")
                    }
                }

                _uiState.update {
                    it.copy(
                        date           = sessionDate,
                        rows           = rowList,
                        isLoading      = false,
                        savedSessionId = null
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /* ---------- 日期选择 ---------- */
    fun onDateSelected(newDate: LocalDate) {
        _uiState.update { it.copy(date = newDate, rows = emptyList(), savedSessionId = null) }
    }

    /* ---------- 行操作 ---------- */

    /** 添加动作：空表直接插 ActionRow；否则 RestRow+ActionRow */
    fun addRow() = _uiState.update { st ->
        val list = st.rows.toMutableList()
        if (list.isNotEmpty()) list += PlanRow.RestRow()         // 先休息
        list += PlanRow.ActionRow()                              // 再动作
        st.copy(rows = list)
    }

    /** 删除动作 idx，同时删掉它前面的 RestRow（若有） */
    fun removeRow(idx: Int) = _uiState.update { st ->
        val list = st.rows.toMutableList()
        if (idx !in list.indices || list[idx] !is PlanRow.ActionRow) return@update st

        list.removeAt(idx)                                       // 先删动作
        val restIdx = idx - 1
        if (restIdx >= 0 && restIdx < list.size && list[restIdx] is PlanRow.RestRow) {
            list.removeAt(restIdx)                               // 再删休息
        }
        st.copy(rows = list)
    }

    /** 更新动作 / 数量 */
    fun updateActionRow(idx: Int, action: String? = null, quantity: String? = null) =
        _uiState.update { st ->
            val list = st.rows.toMutableList()
            val old  = list[idx] as? PlanRow.ActionRow ?: return@update st
            val new  = old.copy(
                action   = action   ?: old.action,
                quantity = quantity ?: old.quantity
            )
            list[idx] = new
            st.copy(rows = list)
        }

    /** 更新休息秒数 */
    fun updateRestRow(idx: Int, restSec: String) = _uiState.update { st ->
        val list = st.rows.toMutableList()
        val old  = list[idx] as? PlanRow.RestRow ?: return@update st
        list[idx] = old.copy(restSeconds = restSec)
        st.copy(rows = list)
    }


    /* ---------- 保存 ---------- */
    fun savePlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, savedSessionId = null) }
            try {
                val st     = _uiState.value
                val list   = st.rows
                val userId = UserSession.uid

                /* 把 ActionRow 提取成 PlanItemCreateDto，并附带其后的休息秒数 */
                val items = buildList {
                    for (i in list.indices) {
                        val r = list[i]
                        if (r is PlanRow.ActionRow) {
                            val rest = list.getOrNull(i + 1)
                                .let { if (it is PlanRow.RestRow) it.restSeconds else "0" }
                                .ifBlank { "0" }
                                .filter { it.isDigit() }
                                .toInt()

                            add(
                                PlanItemCreateDto(
                                    type    = when (r.action) {
                                        "哑铃弯举" -> 1
                                        "卧推"     -> 2
                                        else       -> throw IllegalArgumentException("未知动作：${r.action}")
                                    },
                                    number  = r.quantity.ifBlank { "0" }.toInt(),
                                    tOrder  = size + 1,
                                    tWeight = UserSession.hwWeight ?: 0f,
                                    rest    = rest          // ← 关键：休息写入“上一动作”
                                )
                            )
                        }
                    }
                }

                if (items.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, error = "请至少添加一个动作") }
                    return@launch
                }

                /* 发送请求 */
                val req = PlanDayCreateDto(
                    userId = userId,
                    date   = st.date.format(dateFmt),
                    items  = items
                )

                val created: PlanDayDto = repo.createDayPlan(
                    date  = req.date,
                    items = req.items
                )

                _uiState.update { it.copy(
                    isLoading      = false,
                    savedSessionId = created.session.sessionId
                ) }

            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error     = e.localizedMessage ?: "未知错误"
                ) }
            }
        }
    }
}
