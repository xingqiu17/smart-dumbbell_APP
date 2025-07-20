package com.example.dumb_app.core.network

import com.example.dumb_app.core.model.LoginReq
import com.example.dumb_app.core.model.RegisterReq
import com.example.dumb_app.core.model.UserDto
import retrofit2.http.Body
import retrofit2.http.POST

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

    // 后面可以按需继续添加：上传训练记录、查询计划等接口
}
