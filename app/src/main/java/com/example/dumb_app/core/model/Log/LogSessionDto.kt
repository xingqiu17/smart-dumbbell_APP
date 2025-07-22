package com.example.dumb_app.core.model.Log

/**
 * 对应后端 log_session 表
 */
data class LogSessionDto(
    val recordId: Int? = null,  // 新建时可为空，后端自增
    val userId:   Int,
    val date:     String        // 格式：yyyy-MM-dd
)