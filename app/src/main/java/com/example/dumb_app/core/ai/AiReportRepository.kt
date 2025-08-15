package com.example.dumb_app.core.ai

import kotlinx.coroutines.flow.Flow

interface AiReportRepository {
    /**
     * 以“流”的形式返回 AI 文本增量。UI 端负责把每个增量 append。
     * @param model 默认 "glm-4.5"
     * @param thinkingType "enabled" | "disabled"
     */
    fun streamTrainingAnalysis(
        prompt: String,
        model: String = "glm-4.5",
        thinkingType: String = "enabled"
    ): Flow<String>
}
