package com.example.dumb_app.core.util

/**
 * 保存当前分析会话（可用于后续“复制/分享/再次生成”等）
 * 简单单例，按需扩展（比如换成 Room/Datastore）。
 */
object LogAnalysisSession {
    var recordId: String? = null        // 训练记录ID
    var builtPrompt: String? = null     // 这次发送给模型的完整提示词
    var resultText: String? = null      // 生成结果（最终文本）
}
