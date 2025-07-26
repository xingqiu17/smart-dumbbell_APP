package com.example.dumb_app.core.model.Log

import com.example.dumb_app.core.model.Log.LogWorkCreateReq
data class LogItemCreateReq(
    val type: Int,
    val tOrder: Int,
    val tWeight: Float,
    val num: Int,
    /** 可不传或传 null；后端默认置 0 */
    val avgScore: Int? = null,
    /** 可选；不传表示该 item 暂无 work 明细 */
    val works: List<LogWorkCreateReq>? = null
)