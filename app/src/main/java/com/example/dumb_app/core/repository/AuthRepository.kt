package com.example.dumb_app.core.repository

import com.example.dumb_app.core.model.LoginReq
import com.example.dumb_app.core.model.RegisterReq
import com.example.dumb_app.core.model.UserDto
import com.example.dumb_app.core.network.ApiService

/**
 * 业务数据层：封装所有跟“用户认证”相关的网络调用
 * 只负责在 IO 线程里调用 Retrofit 接口，并把结果返回给上层
 */
class AuthRepository(
    private val api: ApiService
) {

    /** 登录：抛出异常表明失败，返回 UserDto 表示成功 */
    suspend fun login(account: String, password: String): UserDto {
        val req = LoginReq(account = account, password = password)
        return api.login(req)
    }

    /** 注册：抛出异常表明失败，返回 UserDto 表示注册后的用户信息 */
    suspend fun register(req: RegisterReq): UserDto {
        return api.register(req)
    }
}
