package com.example.dumb_app.core.repository

import com.example.dumb_app.core.model.LoginReq
import com.example.dumb_app.core.model.RegisterReq
import com.example.dumb_app.core.model.UserDto
import com.example.dumb_app.core.network.ApiService
import com.example.dumb_app.core.util.PasswordUtil

class AuthRepository(
    private val api: ApiService
) {

    /** 注册：只发 account + 加密密码 */
    suspend fun register(account: String, rawPwd: String): UserDto {
        val md5 = PasswordUtil.md5WithSalt(rawPwd, "dumb")
        val req = RegisterReq(account = account, password = md5)
        return api.register(req)
    }

    /** 登录 */
    suspend fun login(account: String, rawPwd: String): UserDto {
        val md5 = PasswordUtil.md5WithSalt(rawPwd, "dumb")
        val req = LoginReq(account = account, password = md5)
        return api.login(req)
    }
}
