package com.example.dumb_app.core.model.Log

/**
 * 对应后端 log_item 表
 */
data class LogItemDto(
    val groupId:    Int? = null,  // 新建时可为空
    val recordId:   Int,          // 外键，对应 LogSessionDto.recordId
    val type:       Int,
    val tOrder:     Int,
    val tWeight:    Float,
    val num:        Int,
    val avgScore:   Int
)