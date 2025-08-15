package com.example.dumb_app.core.ai

import com.example.dumb_app.core.model.Log.LogDayDto

object AiPromptBuilder {
    /**
     * 基于训练记录构造提示词。已告知启用思考模式，但不要求模型输出思考过程。
     */
    fun buildTrainingAnalysisPrompt(log: LogDayDto): String {
        val sb = StringBuilder()
        sb.appendLine("你是一名专业健身教练与运动数据分析师。请对以下一次训练进行专业分析，并给出可执行的改进建议。")
        sb.appendLine("要求：")
        sb.appendLine("1) 先给出整体总结（强项/薄弱项/风险点）")
        sb.appendLine("2) 按动作逐组分析（代表性评分、完成情况标签统计、幅度/节奏/稳定性）")
        sb.appendLine("3) 给出下一次训练的具体建议（重量、次数、技术要点）")
        sb.appendLine("4) 语言简洁、分点列出，字数控制在 100-150 字")
        sb.appendLine()
        sb.appendLine("【思考模式】thinking.type=enabled（请在内部完成分步推理，不要输出思考过程）")
        sb.appendLine()

        sb.appendLine("【原始数据】")
        sb.appendLine("日期：${log.session?.date ?: "--"}  记录ID：${log.session?.recordId ?: "--"}")
        log.items.sortedBy { it.tOrder }.forEachIndexed { idx, item ->
            sb.appendLine("第${idx + 1}组｜type=${item.type} reps=${item.num} weight=${item.tWeight}kg  avgScore=${item.avgScore}")
        }
        return sb.toString()
    }
}
