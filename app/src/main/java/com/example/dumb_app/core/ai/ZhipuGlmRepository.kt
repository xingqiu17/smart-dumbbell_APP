// app/src/main/java/com/example/dumb_app/core/ai/ZhipuGlmRepository.kt
package com.example.dumb_app.core.ai

import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSource
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * 对接 GLM-4.5 SSE 流式接口：
 * POST /api/paas/v4/chat/completions
 * Header: Authorization: Bearer <token>
 * Body: { model, messages, stream=true, thinking:{type}, ... }
 *
 * 日志说明：
 *  - 统一 TAG = "ZhipuGlmRepo"
 *  - 每次请求生成 requestId，所有日志带 [rid] 前缀，方便串联
 *  - 不打印完整 API Key（做了脱敏）
 */
class ZhipuGlmRepository(
    private val apiKey: String,
    private val http: OkHttpClient = defaultClient(),
    private val endpoint: String = AiConfig.CHAT_COMPLETIONS_URL
) : AiReportRepository {

    override fun streamTrainingAnalysis(
        prompt: String,
        model: String,
        thinkingType: String
    ): Flow<String> = callbackFlow {
        val rid = shortId()
        val bodyJson = buildJsonBody(prompt, model, thinkingType)

        Log.d(TAG, "[$rid] → POST $endpoint")
        Log.d(TAG, "[$rid] headers: Authorization=Bearer ${redact(apiKey)}, Accept=text/event-stream")
        Log.d(TAG, "[$rid] body.preview=${preview(bodyJson)}")

        val body = bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(body)
            .build()

        val call = http.newCall(req)

        // ★ 在 IO 线程里执行整个阻塞请求 + 读取
        val job = launch(Dispatchers.IO) {
            try {
                val resp = call.execute()
                val respBody = resp.body

                Log.d(TAG, "[$rid] ← HTTP ${resp.code} ${resp.message}")
                Log.d(TAG, "[$rid] contentType=${respBody?.contentType()}")

                if (!resp.isSuccessful || respBody == null) {
                    val errText = respBody?.string().orEmpty()
                    Log.e(TAG, "[$rid] error body.preview=${preview(errText)}")
                    throw IllegalStateException("HTTP ${resp.code}: ${resp.message}; body=${preview(errText)}")
                }

                // ★ 先判断是否是 SSE，再决定读取方式（避免先 consume 再 string）
                val ctHeader = resp.header("Content-Type") ?: respBody.contentType()?.toString().orEmpty()
                val isSse = ctHeader.contains("text/event-stream", ignoreCase = true)

                if (isSse) {
                    Log.d(TAG, "[$rid] detected SSE, start line-by-line reading")
                    respBody.use { bodyUse ->
                        val source = bodyUse.source()
                        var lineNo = 0
                        while (!source.exhausted()) {
                            val raw = source.readUtf8Line() ?: continue
                            if (raw.isBlank()) continue
                            lineNo++

                            Log.v(TAG, "[$rid] sse[$lineNo]: ${preview(raw)}")

                            if (!raw.startsWith("data:")) continue
                            val payload = raw.removePrefix("data:").trim()
                            if (payload == "[DONE]") {
                                Log.d(TAG, "[$rid] SSE done")
                                break
                            }
                            val delta = parseDelta(payload)
                            if (delta.isEmpty()) {
                                Log.w(TAG, "[$rid] empty delta parsed from: ${preview(payload)}")
                            } else {
                                trySend(delta)
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "[$rid] not SSE; read whole body as JSON")
                    val whole = respBody.string()
                    Log.v(TAG, "[$rid] whole.preview=${preview(whole)}")
                    val delta = parseDelta(whole)
                    if (delta.isEmpty()) {
                        Log.e(TAG, "[$rid] whole body parse failed (no delta).")
                        throw IllegalStateException("Non-SSE response parse failed. body=${preview(whole)}")
                    } else {
                        trySend(delta)
                    }
                }

                // 正常结束
                close()
            } catch (e: Throwable) {
                Log.e(TAG, "[$rid] exception: ${e.javaClass.simpleName}: ${e.message}", e)
                // 把可视化的错误信息传给 UI
                trySend("[生成失败] ${e.message ?: e.toString()}")
                close(e)
            }
        }

        awaitClose {
            Log.d(TAG, "[$rid] awaitClose -> cancel call & job")
            call.cancel()
            job.cancel()
        }
    }


    private fun buildJsonBody(prompt: String, model: String, thinkingType: String): String {
        val obj = JSONObject().apply {
            put("model", model)
            put("do_sample", true)
            put("stream", true)
            put("thinking", JSONObject().put("type", thinkingType))
            put("temperature", 0.6)
            put("top_p", 0.95)
            put("response_format", JSONObject().put("type", "text"))
            put("messages", JSONArray().put(
                JSONObject().put("role", "user").put("content", prompt)
            ))
            put("request_id", "app-$APP_ID-${shortId()}")
        }
        return obj.toString()
    }

    /**
     * 解析增量文本，兼容：
     *  A) 流式块：choices[0].delta.content
     *  B) 一次性返回：choices[0].message.content
     *  C) 兜底：choices[0].content 或 错误结构
     */
    private fun parseDelta(jsonStr: String): String {
        return try {
            val root = JSONObject(jsonStr)

            if (!root.has("choices")) {
                // 错误结构兜底
                root.optJSONObject("error")?.optString("message")?.let { if (it.isNotEmpty()) return it }
                val msg = root.optString("msg", "")
                if (msg.isNotEmpty()) return msg
                return root.optString("content", "")
            }

            val choices = root.getJSONArray("choices")
            if (choices.length() == 0) return ""

            val ch0 = choices.getJSONObject(0)
            when {
                ch0.has("delta") -> ch0.getJSONObject("delta").optString("content", "")
                ch0.has("message") -> ch0.getJSONObject("message").optString("content", "")
                else -> ch0.optString("content", "")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "parseDelta JSON error: ${t.message}; preview=${preview(jsonStr)}")
            ""
        }
    }

    private fun shortId(): String = UUID.randomUUID().toString().substring(0, 8)

    private fun redact(s: String): String =
        if (s.length >= 12) s.take(6) + "…" + s.takeLast(4) else "***"

    private fun preview(s: String, max: Int = 400): String =
        s.replace("\n", "⏎").replace("\r", "").take(max)

    companion object {
        private const val TAG = "ZhipuGlmRepo"
        private const val APP_ID = "dumb-app"

        private fun defaultClient(): OkHttpClient {
            val builder = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS) // SSE 长连不超时

            // 用 AiConfig 的开关控制网络日志
            if (AiConfig.DEBUG_LOG) {
                val logger = HttpLoggingInterceptor { msg -> Log.d(TAG, msg) }
                logger.level = HttpLoggingInterceptor.Level.BODY
                builder.addInterceptor(logger)
            }
            return builder.build()
        }
    }
}
