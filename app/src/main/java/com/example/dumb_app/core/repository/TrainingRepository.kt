package com.example.dumb_app.core.repository

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

    /* ---------- 查询：某用户某日完整计划 ---------- */
    suspend fun getDayPlan(date: String): PlanDayDto? {
        val uid = UserSession.uid
        return runCatching { api.getDayPlan(uid, date) }
            .getOrNull()                 // 若当天无计划，后端返回 404 → 转换为 null
    }

    /* ---------- 创建 / 覆盖：某日训练计划 ---------- */
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

    /* ---------- 仅按 sessionId 拉动作明细（通常不用调，因为 getDayPlan 已返回明细） ---------- */
    suspend fun listItemsBySession(sessionId: Int): List<PlanItemDto> =
        api.listItemsBySession(sessionId)
}
