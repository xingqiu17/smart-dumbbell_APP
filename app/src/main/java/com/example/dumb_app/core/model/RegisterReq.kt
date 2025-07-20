package com.example.dumb_app.core.model


data class RegisterReq(
    val account: String,
    val password: String        // ← 这里填加盐后的 MD5
)
