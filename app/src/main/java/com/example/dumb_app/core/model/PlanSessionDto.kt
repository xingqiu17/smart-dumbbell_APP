package com.example.dumb_app.core.model

/**
 * 对应后端 plan_session 表
 */
data class PlanSessionDto(
    val sessionId: Int? = null,   // 新建时可为空，后端自增
    val userId:    Int,
    val date:      String,        // yyyy-MM-dd
    val complete:  Boolean        // 0=未完成 1=已完成
)
