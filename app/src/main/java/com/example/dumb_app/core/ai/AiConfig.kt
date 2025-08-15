// app/src/main/java/com/example/dumb_app/core/ai/AiConfig.kt
package com.example.dumb_app.core.ai

object AiConfig {
    // TODO: 把它替换为你的真实 Token（先这样写在 App 内，之后再迁到服务端）
    const val ZHIPU_API_KEY: String = "c304b99dcac9452c908a9f8cee651585.VfFCZpN6cPnVT6oC"

    // 官方对话补全（Chat Completions）SSE 接口
    const val CHAT_COMPLETIONS_URL: String =
        "https://open.bigmodel.cn/api/paas/v4/chat/completions"

    // 日志总开关（调试时 true，上线建议设为 false）
    const val DEBUG_LOG: Boolean = true
}
