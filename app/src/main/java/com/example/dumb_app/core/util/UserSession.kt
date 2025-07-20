package com.example.dumb_app.core.util

import com.example.dumb_app.core.model.UserDto

/**
 * 轻量级全局会话：缓存当前登录 / 注册成功后的用户。
 * 仅保存内存，不做持久化；如需长期存储可再接入 DataStore。
 */
object UserSession {
    var currentUser: UserDto? = null
        private set

    fun update(user: UserDto) {
        currentUser = user
    }

    val uid: Int
        get() = currentUser?.id
            ?: error("User not logged in — uid is null")
}
