package com.example.dumb_app.core.util

import com.example.dumb_app.core.network.NetworkModule
import com.example.dumb_app.core.repository.AuthRepository

/**
 * 简单单例容器：统一管理各 Repository 实例
 */
object ServiceLocator {
    val authRepository: AuthRepository by lazy {
        AuthRepository(NetworkModule.apiService)
    }
}
