package com.example.dumb_app.core.model

data class PlanDayDto(
    val session: PlanSessionDto,
    val items:   List<PlanItemDto>
)