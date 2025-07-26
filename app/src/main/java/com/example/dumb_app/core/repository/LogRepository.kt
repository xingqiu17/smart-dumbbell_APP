package com.example.dumb_app.core.repository

import com.example.dumb_app.core.model.Log.LogDayDto
import com.example.dumb_app.core.model.Log.LogItemDto
import com.example.dumb_app.core.model.Log.LogWorkDto
import com.example.dumb_app.core.model.Log.LogDayCreateReq
import com.example.dumb_app.core.network.ApiService
import com.example.dumb_app.core.util.UserSession

/**
 * 训练记录（头 + 明细 + 动作）数据仓库
 */
class LogRepository(
    private val api: ApiService
) {
    /**
     * 创建【一次训练记录：session + items + works】。
     * 不会覆盖同日已有记录，后端每次都会新增一条 session。
     */
    suspend fun createDayRecord(req: LogDayCreateReq): LogDayDto =
        api.createDayRecord(req)

    /**
     * 查询【用户 + 日期】的全部训练记录（每条带 items）。
     * 后端返回数组；若无数据或 204，返回空列表。
     */
    suspend fun getDayRecordsAll(date: String): List<LogDayDto> =
        runCatching { api.getDayRecords(UserSession.uid, date) }
            .getOrElse { emptyList() }

    /**
     * 为兼容你之前“只拿单条”的用法：
     * 从当天所有记录里挑“最新的一条”（按 recordId 最大）返回。
     * 若无数据或出错，返回 null。
     */
    suspend fun getDayRecords(date: String): LogDayDto? =
        getDayRecordsAll(date)
            .maxByOrNull { it.session.recordId ?: Int.MIN_VALUE }

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
