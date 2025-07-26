package com.example.dumb_app.core.repository

import com.example.dumb_app.core.model.Plan.CompleteReq
import com.example.dumb_app.core.model.Plan.PlanDayCreateDto
import com.example.dumb_app.core.model.Plan.PlanDayDto
import com.example.dumb_app.core.model.Plan.PlanItemCreateDto
import com.example.dumb_app.core.model.Plan.PlanItemDto
import com.example.dumb_app.core.network.ApiService
import com.example.dumb_app.core.util.UserSession

/**
 * 训练计划（头 + 明细）数据仓库
 */
class TrainingRepository(
    private val api: ApiService
) {
    // ---------- 查询：某用户某日完整计划 ----------
    suspend fun getDayPlans(date: String): List<PlanDayDto> =
        api.getDayPlans(UserSession.uid, date)

    // ---------- 创建：某日训练计划 ----------
    suspend fun createDayPlan(
        date: String,
        items: List<PlanItemCreateDto>
    ): PlanDayDto {
        val req = PlanDayCreateDto(
            userId = UserSession.uid,
            date   = date,
            items  = items
        )
        return api.createDayPlan(req)
    }

    // ---------- 完整更新：覆盖某日训练计划（头 + 明细） ----------
    suspend fun updateDayPlan(
        sessionId: Int,
        date:      String,
        items:     List<PlanItemCreateDto>
    ): PlanDayDto {
        val req = PlanDayCreateDto(
            userId = UserSession.uid,
            date   = date,
            items  = items
        )
        return api.updateDayPlan(sessionId, req)
    }

    // ---------- 部分更新：只修改 complete 标志 ----------
    suspend fun completePlan(
        sessionId: Int,
        complete: Boolean
    ) {
        val req = CompleteReq(complete = complete)
        api.updatePlanComplete(sessionId, req)
    }

    // ---------- 删除：移除整个计划（头 + 明细） ----------
    suspend fun deleteDayPlan(sessionId: Int) {
        api.deleteDayPlan(sessionId)
    }

    // ---------- 可选：仅按 sessionId 拉动作明细 ----------
    suspend fun listItemsBySession(sessionId: Int): List<PlanItemDto> =
        api.listItemsBySession(sessionId)
}
