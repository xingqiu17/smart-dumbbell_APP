package com.example.dumb_app.core.model.Plan

import com.google.gson.annotations.SerializedName

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
    @SerializedName("itemId")   val itemId: Int? = null,
    @SerializedName("session")  val session: PlanSessionDto? = null, // ← 接收嵌套对象，别用 sessionId
    @SerializedName("type")     val type: Int,
    @SerializedName("number")   val number: Int,
    @SerializedName("torder")   val tOrder: Int,     // 小写键 → 驼峰字段，必须加 @SerializedName
    @SerializedName("tweight")  val tWeight: Float,  // 同上；服务端返回 2 也能转成 2.0
    @SerializedName("complete") val complete: Boolean
)
