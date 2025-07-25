package com.example.dumb_app.core.model.Plan

data class PlanDayCreateDto(
    val userId: Int,
    val date:   String,                 // yyyy-MM-dd
    val items:  List<PlanItemCreateDto>
)