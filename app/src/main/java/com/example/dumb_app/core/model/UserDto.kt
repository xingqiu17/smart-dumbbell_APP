package com.example.dumb_app.core.model
import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("userId")  // ← 后端字段是 userId
    val id: Int,
    val account: String,
    val name: String,
    val gender: Int,
    val height: Float,
    val weight: Float,
    val birthday: String,
    val aim: Int,
    val hwId: Int,
    val hwWeight: Float
)