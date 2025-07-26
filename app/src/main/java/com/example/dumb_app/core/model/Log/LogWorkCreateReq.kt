package com.example.dumb_app.core.model.Log

data class LogWorkCreateReq(
    val acOrder: Int,
    /** 可不传或传 null；后端默认置 0 */
    val score: Int? = null
)