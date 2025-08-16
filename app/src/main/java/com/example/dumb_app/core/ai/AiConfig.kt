package com.example.dumb_app.core.ai

object AiConfig {
    // TODO: 写入你的真实 Key（不用加 Bearer 前缀）
    const val ZHIPU_API_KEY: String = "c304b99dcac9452c908a9f8cee651585.VfFCZpN6cPnVT6oC"

    const val CHAT_COMPLETIONS_URL: String =
        "https://open.bigmodel.cn/api/paas/v4/chat/completions"

    // 调试日志
    const val DEBUG_LOG: Boolean = true

    // 为避免“只显示一半”，拉高输出上限
    const val MAX_TOKENS: Int = 2048

    // 若你想“慢速打字机效果”，可设为 >0（毫秒）。默认 0，表示尽快渲染。
    const val STREAM_CHAR_DELAY_MS: Long = 0L
}
