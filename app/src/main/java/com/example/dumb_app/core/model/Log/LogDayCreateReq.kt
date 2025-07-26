package com.example.dumb_app.core.model.Log

import com.example.dumb_app.core.model.Log.LogItemCreateReq

/** POST /api/log/session 请求体 */
data class LogDayCreateReq(
    val userId: Int,
    /** yyyy-MM-dd */
    val date: String,
    val items: List<LogItemCreateReq>
)