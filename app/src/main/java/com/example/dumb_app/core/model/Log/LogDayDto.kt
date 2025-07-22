package com.example.dumb_app.core.model.Log

data class LogDayDto(
    val session: LogSessionDto,
    val items:   List<LogItemDto>,
    // 1）给它一个默认值，JSON 里没这字段就用空 Map
    val works:   Map<Int, List<LogWorkDto>> = emptyMap()
)
