package com.example.dumb_app.core.model

data class UserDto(
    val id: Int,
    val account: String,
    val name: String,
    val gender: Int,
    val height: Float,
    val weight: Float,
    val birthday: String,
    val aim: Int,
    val hwId: Int
)