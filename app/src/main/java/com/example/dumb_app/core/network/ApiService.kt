package com.example.dumb_app.core.network

import com.example.dumb_app.core.model.User.LoginReq
import com.example.dumb_app.core.model.User.RegisterReq
import com.example.dumb_app.core.model.User.UserDto
import com.example.dumb_app.core.model.User.TrainDataReq
import com.example.dumb_app.core.model.User.UpdateNameReq
import com.example.dumb_app.core.model.User.BodyDataReq
import com.example.dumb_app.core.model.Plan.PlanItemDto
import com.example.dumb_app.core.model.Plan.PlanDayCreateDto
import com.example.dumb_app.core.model.Plan.PlanDayDto
import com.example.dumb_app.core.model.Log.LogDayDto
import com.example.dumb_app.core.model.Log.LogItemDto
import com.example.dumb_app.core.model.Log.LogSessionDto
import com.example.dumb_app.core.model.Log.LogWorkDto
import com.example.dumb_app.core.model.Plan.CompleteReq
import com.example.dumb_app.core.model.Log.LogDayCreateReq

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Query

/**
 * 注意：你的 BASE_URL 里已包含 "/api/"，所以下面的相对路径会拼成 /api/xxx
 */
interface ApiService {

    /** 注册新用户 */
    @POST("v1/users/register")
    suspend fun register(@Body req: RegisterReq): UserDto

    /** 登录 */
    @POST("v1/users/login")
    suspend fun login(@Body req: LoginReq): UserDto

    /** 更新训练数据（POST，不是 PATCH）*/
    @POST("v1/users/{id}/trainData")
    suspend fun updateTrainData(
        @Path("id") id: Int,
        @Body req: TrainDataReq
    ): UserDto

    /** 修改用户名 */
    @POST("v1/users/{id}/name")
    suspend fun updateName(
        @Path("id") id: Int,
        @Body req: UpdateNameReq
    ): UserDto

    /** 修改身体数据：出生日期、身高、体重、性别 */
    @POST("v1/users/{id}/body")
    suspend fun updateBodyData(
        @Path("id") id: Int,
        @Body req: BodyDataReq
    ): UserDto

    /** ======== 训练计划（保持原样） ======== */

    /** 查询【用户 + 日期】的一天完整训练计划（头 + 明细） */
    @GET("plan/session/day")
    suspend fun getDayPlans(
        @Query("userId") userId: Int,
        @Query("date")   date:   String
    ): List<PlanDayDto>

    /** 创建 / 覆盖某天的训练计划 */
    @POST("plan/session")
    suspend fun createDayPlan(
        @Body req: PlanDayCreateDto
    ): PlanDayDto

    /** 仅按 sessionId 拉取动作明细（如果 getDayPlan 已含明细，可不必再调） */
    @GET("plan/item/list")
    suspend fun listItemsBySession(
        @Query("sessionId") sessionId: Int
    ): List<PlanItemDto>

    /** ① 完整更新某个计划（修改头 + 明细） */
    @PUT("plan/session/{sessionId}")
    suspend fun updateDayPlan(
        @Path("sessionId") sessionId: Int,
        @Body req: PlanDayCreateDto
    ): PlanDayDto

    /** ② 仅修改 complete 标志 */
    @PATCH("plan/session/{sessionId}/complete")
    suspend fun updatePlanComplete(
        @Path("sessionId") sessionId: Int,
        @Body req: CompleteReq
    ): Unit

    /** ③ 删除整个计划 */
    @DELETE("plan/session/{sessionId}")
    suspend fun deleteDayPlan(
        @Path("sessionId") sessionId: Int
    ): Unit

    /** ======== 训练记录（头 + 明细 + 动作） ======== */

    /** 1) 创建【一次训练记录：session + items + works】（不覆盖当日旧记录） */
    @POST("log/session")
    suspend fun createDayRecord(
        @Body req: LogDayCreateReq
    ): LogDayDto   // 复用你已有的聚合 DTO：包含 session + items

    /** 2) 按日查询【用户 + 日期】的所有训练记录（每条带 items） */
    @GET("log/session/day")
    suspend fun getDayRecords(
        @Query("userId") userId: Int,
        @Query("date")   date:   String      // yyyy-MM-dd
    ): List<LogDayDto>

    /** 3) 查询单条训练记录的所有运动组（LogItem） */
    @GET("log/item/session/{recordId}")
    suspend fun listItemsByRecord(
        @Path("recordId") recordId: Int
    ): List<LogItemDto>

    /** 4) 查询单个运动组下所有动作明细（LogWork） */
    @GET("log/work/item/{groupId}")
    suspend fun listWorksByGroup(
        @Path("groupId") groupId: Int
    ): List<LogWorkDto>
}
