package com.example.dumb_app.core.repository

import com.example.dumb_app.core.model.User.BodyDataReq
import com.example.dumb_app.core.model.User.LoginReq
import com.example.dumb_app.core.model.User.RegisterReq
import com.example.dumb_app.core.model.User.TrainDataReq
import com.example.dumb_app.core.model.User.UpdateNameReq
import com.example.dumb_app.core.model.User.UserDto
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
        UserSession.update(user)
        return user
    }

    /** 登录 */
    suspend fun login(account: String, rawPwd: String): UserDto {
        val md5 = PasswordUtil.md5WithSalt(rawPwd, "dumb")
        val user = api.login(LoginReq(account, md5))
        UserSession.update(user)
        UserSession.updateHwWeight(user.hwWeight)
        return user
    }

    /** 更新训练数据：直接用缓存里的 uid */
    suspend fun updateTrainData(aim: Int, hwWeight: Float): UserDto {
        val uid = UserSession.uid
        val req = TrainDataReq(aim, hwWeight)
        val updated = api.updateTrainData(uid, req)
        UserSession.update(updated)
        UserSession.updateHwWeight(hwWeight)
        return updated
    }

    /** 更新用户名 */
    suspend fun updateName(newName: String): UserDto {
        val uid = UserSession.uid
        val req = UpdateNameReq(name = newName)
        val updated = api.updateName(uid, req)
        UserSession.update(updated)
        return updated
    }

    /** 更新身体数据 */
    suspend fun updateBodyData(birthday: String, height: Float, weight: Float, gender: Int): UserDto {
        val uid = UserSession.uid
        val req = BodyDataReq(
            birthday = birthday,
            height   = height,
            weight   = weight,
            gender   = gender
        )
        val updated = api.updateBodyData(uid, req)
        UserSession.update(updated)
        return updated
    }
}
