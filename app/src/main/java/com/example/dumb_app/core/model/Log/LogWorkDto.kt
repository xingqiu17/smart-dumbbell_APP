package com.example.dumb_app.core.model.Log

/**
 * 对应后端 log_work 表
 */
data class LogWorkDto(
    val actionId: Int? = null,   // 新建时可为空
    val groupId:  Int,           // 外键，对应 LogItemDto.groupId
    val acOrder:  Int,
    val score:    Int,
    //val performance: Int
)