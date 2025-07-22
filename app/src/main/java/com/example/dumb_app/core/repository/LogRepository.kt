package com.example.dumb_app.core.repository

import com.example.dumb_app.core.model.Log.LogDayDto
import com.example.dumb_app.core.model.Log.LogItemDto
import com.example.dumb_app.core.model.Log.LogWorkDto
import com.example.dumb_app.core.network.ApiService
import com.example.dumb_app.core.util.UserSession

/**
 * 训练记录（头 + 明细 + 动作）数据仓库
 */
class LogRepository(
    private val api: ApiService
) {
    /**
     * 查询某用户某日的训练记录（单条 Session + Items + WorksMap）
     * 如果当天无记录，后端可能返回 204 或抛异常 → 这里 catch 后返回 null
     */
    suspend fun getDayRecords(date: String): LogDayDto? =
        runCatching { api.getDayRecords(UserSession.uid, date) }
            .getOrNull()

    /**
     * 查询某次训练记录下的所有运动组（LogItem）
     */
    suspend fun listItemsByRecord(recordId: Int): List<LogItemDto> =
        api.listItemsByRecord(recordId)

    /**
     * 查询某个运动组下所有动作明细（LogWork）
     */
    suspend fun listWorksByGroup(groupId: Int): List<LogWorkDto> =
        api.listWorksByGroup(groupId)
}
