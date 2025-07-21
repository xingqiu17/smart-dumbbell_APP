package com.example.dumb_app.core.util

import com.example.dumb_app.core.network.NetworkModule
import com.example.dumb_app.core.repository.AuthRepository
import com.example.dumb_app.core.repository.TrainingRepository
/**
 * 简单单例容器：统一管理各 Repository 实例
 */
object ServiceLocator {
    val authRepository: AuthRepository by lazy {
        AuthRepository(NetworkModule.apiService)
    }

    val trainingRepository: TrainingRepository by lazy {
        TrainingRepository(NetworkModule.apiService)
    }
}
