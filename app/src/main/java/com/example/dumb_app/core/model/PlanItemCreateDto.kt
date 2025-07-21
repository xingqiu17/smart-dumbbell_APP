package com.example.dumb_app.core.model

data class PlanItemCreateDto(
    val type:    Int,
    val number:  Int,
    val tOrder:  Int,
    val tWeight: Int
)