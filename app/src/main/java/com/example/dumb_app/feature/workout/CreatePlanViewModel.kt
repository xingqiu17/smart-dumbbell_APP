package com.example.dumb_app.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dumb_app.core.model.Plan.PlanDayCreateDto
import com.example.dumb_app.core.model.Plan.PlanItemCreateDto
import com.example.dumb_app.core.model.Plan.PlanDayDto
import com.example.dumb_app.core.repository.TrainingRepository
import com.example.dumb_app.core.util.UserSession
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// UI 用行数据
data class RowData(val action: String = "", val quantity: String = "")

// UI 状态
data class CreatePlanUiState(
    val date: LocalDate = LocalDate.now(),
    val rows: List<RowData> = emptyList(),
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

    /** 加载当天已有计划（如果有） */
    fun loadExistingPlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val dateStr = _uiState.value.date.format(dateFmt)
            // 获取同一天所有计划
            val plans: List<PlanDayDto> = repo.getDayPlans(dateStr)

            if (plans.isNotEmpty()) {
                // 选择最新创建的那条（sessionId 最大）
                val plan = plans.maxByOrNull { it.session.sessionId ?: 0 }!!
                val session = plan.session
                val planDate = LocalDate.parse(session.date, dateFmt)

                // 将 PlanItemDto 映射为 RowData
                val rows = plan.items.map { item ->
                    val actionName = when (item.type) {
                        1 -> "哑铃弯举"
                        2 -> "卧推"
                        else -> ""
                    }
                    RowData(
                        action   = actionName,
                        quantity = item.number.toString()
                    )
                }

                _uiState.update {
                    it.copy(
                        date           = planDate,
                        rows           = rows,
                        isLoading      = false,
                        savedSessionId = null    // 保持 null，新建时才赋值
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }


    /** 用户选了新日期 */
    fun onDateSelected(newDate: LocalDate) {
        _uiState.update { it.copy(date = newDate, rows = emptyList(), savedSessionId = null) }
    }

    /** 增删改行 */
    fun addRow() = _uiState.update { it.copy(rows = it.rows + RowData()) }
    fun removeRow(idx: Int) = _uiState.update {
        it.copy(rows = it.rows.toMutableList().also { list -> list.removeAt(idx) })
    }
    fun updateRow(idx: Int, action: String? = null, quantity: String? = null) {
        _uiState.update { st ->
            val list = st.rows.toMutableList()
            val old = list[idx]
            list[idx] = old.copy(
                action   = action ?: old.action,
                quantity = quantity ?: old.quantity
            )
            st.copy(rows = list)
        }
    }

    /** 保存（创建或覆盖） */
    fun savePlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, savedSessionId = null) }
            try {
                val userId = UserSession.uid                      // ← 直接取当前登录用户
                val st     = _uiState.value
                val items  = st.rows.mapIndexed { idx, row ->
                    PlanItemCreateDto(
                        type    = when (row.action) {
                            "哑铃弯举" -> 1
                            "卧推"     -> 2
                            else       -> throw IllegalArgumentException("未知动作：${row.action}")
                        },
                        number  = row.quantity.toIntOrNull() ?: 0,
                        tOrder  = idx + 1,
                        tWeight = 0.0f
                    )
                }

                // 构造完整请求 DTO，带上 userId
                val req = PlanDayCreateDto(
                    userId = userId,
                    date   = st.date.format(dateFmt),
                    items  = items
                )

                val created: PlanDayDto = repo.createDayPlan(
                    date  = st.date.format(dateFmt),
                    items = items
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
