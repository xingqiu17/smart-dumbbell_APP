// app/src/main/java/com/example/dumb_app/core/model/BodyDataReq.kt

package com.example.dumb_app.core.model

/**
 * 与后端 {@code BodyDataReq} 对应。
 * birthday 按 ISO_DATE 格式 (yyyy-MM-dd) 传递。
 */
data class BodyDataReq(
    val birthday: String,
    val height: Float,
    val weight: Float,
    val gender: Int
)
