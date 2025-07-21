package com.example.dumb_app.core.network

import com.example.dumb_app.core.model.LoginReq
import com.example.dumb_app.core.model.RegisterReq
import com.example.dumb_app.core.model.UserDto
import com.example.dumb_app.core.model.TrainDataReq
import com.example.dumb_app.core.model.UpdateNameReq
import com.example.dumb_app.core.model.BodyDataReq
import com.example.dumb_app.core.model.PlanItemDto
import com.example.dumb_app.core.model.PlanDayCreateDto
import com.example.dumb_app.core.model.PlanDayDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 定义后端 HTTP 接口。
 * 注意：BASE_URL + POST("v1/users/...") 构成完整路径： http://host:port/api/v1/users/...
 */
interface ApiService {

    /** 注册新用户 */
    @POST("v1/users/register")
    suspend fun register(@Body req: RegisterReq): UserDto

    /** 登录 */
    @POST("v1/users/login")
    suspend fun login(@Body req: LoginReq): UserDto


        /** 更新训练数据（目标 + 配重） */
    /** 更新训练数据（POST，不是 PATCH）*/
    @POST("v1/users/{id}/trainData")         // ← 保持 @Path 形式
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

    /** 查询【用户 + 日期】的一天完整训练计划（头 + 明细） */
    @GET("plan/session/day")
    suspend fun getDayPlan(
        @Query("userId") userId: Int,
        @Query("date")   date:   String        // yyyy-MM-dd
    ): PlanDayDto

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

    // 后面可以按需继续添加：上传训练记录、查询计划等接口
}
