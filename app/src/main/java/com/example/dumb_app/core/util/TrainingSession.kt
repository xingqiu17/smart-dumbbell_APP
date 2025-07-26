package com.example.dumb_app.core.util

import com.example.dumb_app.core.model.Plan.PlanItemDto

/**
 * 进程内的“当前训练会话”缓存（与 UserSession 风格一致）
 * 仅内存保存；进程被杀会丢失
 */
object TrainingSession {
    var sessionId: Int? = null
        private set

    var items: List<PlanItemDto> = emptyList()
        private set

    // 可选：用于在训练页标题上显示，例如 “计划 1”
    var title: String? = null
        private set

    /** 设置/更新当前训练会话（点击“开始训练”时调用） */
    fun update(sessionId: Int, items: List<PlanItemDto>, title: String? = null) {
        this.sessionId = sessionId
        this.items = items
        this.title = title
    }

    /** 清空（结束/退出训练时可调用） */
    fun clear() {
        sessionId = null
        items = emptyList()
        title = null
    }

    /** 必须存在的会话 ID；不存在时抛错 */
    val sid: Int
        get() = sessionId ?: error("No active training session — sessionId is null")

    /** 兜底使用的目标次数（取第一组动作的次数） */
    val firstTargetReps: Int?
        get() = items.firstOrNull()?.number

    /** 本次计划共多少组 */
    val totalSets: Int
        get() = items.size
}
