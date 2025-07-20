package com.example.dumb_app.core.repository

import com.example.dumb_app.core.model.*
import com.example.dumb_app.core.network.ApiService
import com.example.dumb_app.core.util.PasswordUtil
import com.example.dumb_app.core.util.UserSession

class AuthRepository(
    private val api: ApiService
) {

    /** 注册：只发 account + (盐=dumb)MD5 密码 */
    suspend fun register(account: String, rawPwd: String): UserDto {
        val md5 = PasswordUtil.md5WithSalt(rawPwd, "dumb")
        val user = api.register(RegisterReq(account = account, password = md5))
        UserSession.update(user)                 // ← 缓存
        return user
    }

    /** 登录 */
    suspend fun login(account: String, rawPwd: String): UserDto {
        val md5 = PasswordUtil.md5WithSalt(rawPwd, "dumb")
        val user = api.login(LoginReq(account, md5))
        UserSession.update(user)                 // ← 缓存
        return user
    }

    /** 更新训练数据：直接用缓存里的 uid */
    suspend fun updateTrainData(aim: Int, weight: Float): UserDto {
        val uid = UserSession.uid                   // ← 这里拿到真正 id
        val req = TrainDataReq(aim, weight)
        return api.updateTrainData(uid, req)        // ← POST /users/{uid}/trainData
    }
}
