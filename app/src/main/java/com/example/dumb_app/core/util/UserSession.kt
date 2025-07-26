package com.example.dumb_app.core.util

import com.example.dumb_app.core.model.User.UserDto

object UserSession {
    var currentUser: UserDto? = null
        private set

    // 额外缓存当前配重（仅内存）
    var hwWeight: Float? = null
        private set

    fun update(user: UserDto) {
        currentUser = user
        // 如果你的 UserDto 自身带有配重字段，可以在这里顺便写入：
         hwWeight = user.hwWeight
    }

    // 显式更新配重（修改配重后调用）
    fun updateHwWeight(newHw: Float) {
        hwWeight = newHw
    }

    val uid: Int
        get() = currentUser?.id
            ?: error("User not logged in — uid is null")
}
