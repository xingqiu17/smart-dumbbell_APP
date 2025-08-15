package com.example.dumb_app.core.model.Log

import com.google.gson.annotations.SerializedName

/**
 * 对应后端 log_item 表
 */
data class LogItemDto(
    val groupId:    Int? = null,  // 新建时可为空
    val recordId:   Int,          // 外键，对应 LogSessionDto.recordId
    val type:       Int,
    val tOrder:     Int,
    @SerializedName(value = "tWeight", alternate = ["weight","tweight","t_weight"])
    val tWeight:    Float,
    @SerializedName(value = "num", alternate = ["number"])
    val num:        Int,
    val avgScore:   Int
)