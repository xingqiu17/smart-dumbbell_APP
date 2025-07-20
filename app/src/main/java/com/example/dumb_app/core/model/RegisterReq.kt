package com.example.dumb_app.core.model

data class RegisterReq(
    val hwId: Int         = 0,
    val account: String,
    val password: String,
    val name: String      = account,
    val gender: Int       = 0,
    val height: Float     = 0f,
    val weight: Float     = 0f,
    val birthday: String  = "1970-01-01",
    val aim: Int          = 0
)
