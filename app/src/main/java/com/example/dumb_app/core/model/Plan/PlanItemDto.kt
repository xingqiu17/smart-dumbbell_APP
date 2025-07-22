package com.example.dumb_app.core.model.Plan

/**
 * 对应后端 plan_item 表
 *
 * 说明：后端返回的 JSON 中，PlanItem 里可能包含一个完整的
 *       "session": { sessionId: ..., userId: ..., ... } 对象。
 *       为了避免解析复杂，这里仅保留 sessionId 字段；
 *       如果后端确实返回嵌套对象，可在 Retrofit 的 Converter
 *       做自定义映射，或再加一个 PlanSessionLite。
 */
data class PlanItemDto(
    val itemId:    Int? = null,   // 新建时可为空
    val sessionId: Int,           // 外键
    val type:      Int,
    val number:    Int,
    val tOrder:    Int,
    val tWeight:   Int,
    val complete:  Boolean
)
