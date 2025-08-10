package com.example.dumb_app.core.connectivity.wifi

import android.app.Application
import android.net.wifi.ScanResult
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dumb_app.core.repository.PairingRepository
import com.example.dumb_app.core.util.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

import okhttp3.*

import java.util.UUID

class WifiScanViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG_WSVM = "WifiScanViewModel"
    }
    private val scanner = WifiScanner(application)
    private val repo    = PairingRepository(application)

    // 扫描结果
    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanResults: StateFlow<List<ScanResult>> = _scanResults
    data class TrainingPlanItem(val type: Int, val reps: Int, val weight: Double, val rest: Int)

    // WS 事件
    sealed class WsEvent {
        object PairRequested : WsEvent()
        object Paired       : WsEvent()
        object Connected    : WsEvent()
        object RestSkipped : WsEvent()
        object TrainingExited : WsEvent()
        data class Error(val msg: String): WsEvent()
        data class Data(val payload: String): WsEvent()
        data class ExerciseData(val exercise: Int, val rep: Int, val score: Double) : WsEvent()
        data class TrainingStarted(
            val sessionId: Long,
            val date: String,
            val items: List<TrainingPlanItem>
        ) : WsEvent()
    }
    private val _wsEvents = MutableStateFlow<WsEvent?>(null)
    val wsEvents: StateFlow<WsEvent?> = _wsEvents

    private var webSocket: WebSocket? = null
    private var currentHost: String? = null   // ← 新增

    fun startScan() {
        scanner.startScan { results ->
            viewModelScope.launch { _scanResults.value = results }
        }
    }

    /** 新增：取上次 host，用于自动重连 */
    fun getLastHost(): String? = repo.getLastHost()

    fun connectToDevice(ip: String) {
        currentHost = ip  // ← 记录当前尝试的 host
        viewModelScope.launch(Dispatchers.IO) {
            val baseUrl = "ws://$ip:8080/ws"
            val token = repo.getToken()
            val url = if (token != null) "$baseUrl?token=$token" else baseUrl

            val request = Request.Builder().url(url).build()
            val client  = OkHttpClient()
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    viewModelScope.launch {
                        when {
                            text.contains("\"event\":\"pair_request\"") -> {
                                _wsEvents.value = WsEvent.PairRequested
                                val newToken = repo.getToken() ?: UUID.randomUUID().toString()
                                repo.saveToken(newToken)
                                webSocket?.send("""{"event":"pair_response","token":"$newToken"}""")
                            }
                            text.contains("\"event\":\"paired\"") -> {
                                _wsEvents.value = WsEvent.Paired
                                // ← 配对完成后也可当作“已知设备”存 host
                                currentHost?.let { repo.saveLastHost(it) }
                                sendUserBindIfLoggedIn()
                            }
                            text.contains("\"event\":\"connected\"") -> {
                                _wsEvents.value = WsEvent.Connected
                                // ← 自动重连成功
                                currentHost?.let { repo.saveLastHost(it) }
                                sendUserBindIfLoggedIn()
                            }
                            text.contains("\"event\":\"rest_skipped\"") -> {
                                _wsEvents.value = WsEvent.RestSkipped
                            }
                            text.contains("\"event\":\"training_exited\"") -> {
                                _wsEvents.value = WsEvent.TrainingExited
                            }
                            text.contains("\"event\":\"training_started\"") -> {
                                try {
                                    val json = JSONObject(text)
                                    val itemsJson = json.getJSONArray("items")
                                    val itemsList = mutableListOf<TrainingPlanItem>()
                                    for (i in 0 until itemsJson.length()) {
                                        val itemObj = itemsJson.getJSONObject(i)
                                        itemsList.add(
                                            TrainingPlanItem(
                                                type = itemObj.getInt("type"),
                                                reps = itemObj.getInt("reps"),
                                                weight = itemObj.getDouble("weight"),
                                                rest = itemObj.getInt("rest")
                                            )
                                        )
                                    }
                                    _wsEvents.value = WsEvent.TrainingStarted(
                                        sessionId = json.getLong("sessionId"),
                                        date = json.getString("date"),
                                        items = itemsList
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG_WSVM, "Failed to parse training_started event", e)
                                    // 解析失败，可以发送一个错误事件
                                    _wsEvents.value = WsEvent.Error("解析训练计划失败: ${e.message}")
                                }
                            }
                            text.contains("\"event\":\"rep_data\"") -> {
                                // 解析 "rep_data" 数据
                                val jsonObject = JSONObject(text)
                                val exercise = jsonObject.getInt("exercise")
                                val rep = jsonObject.getInt("rep")
                                val score = jsonObject.getDouble("score")

                                // 更新 StateFlow
                                _wsEvents.value = WsEvent.ExerciseData(exercise, rep, score)
                            }
                            // 过滤掉包含 "setUser" 和 "user_bound" 的消息，不更新 _wsEvents
                            text.contains("\"event\":\"setUser\"") || text.contains("\"event\":\"user_bound\"") -> {
                                return@launch
                            }
                            else -> {
                                // 处理其他正常数据
                                _wsEvents.value = WsEvent.Data(text)
                            }

                        }
                    }
                }

                override fun onFailure(ws: WebSocket, t: Throwable, resp: Response?) {
                    viewModelScope.launch {
                        _wsEvents.value = WsEvent.Error(t.message ?: "Unknown")
                    }
                }
            })

        }
    }

    /** 新增：在外面调用，发送文本消息给设备 */
    fun sendMessage(text: String) {
        Log.d(TAG_WSVM, "sendMessage -> $text")
        viewModelScope.launch(Dispatchers.IO) {
            webSocket?.send(text)
        }
    }

    fun sendSkipRest() {
        val payload = "{\"event\":\"skip_rest\"}"
        Log.d(TAG_WSVM, "sendSkipRest")
        sendMessage(payload)
    }

    fun sendExitTraining() {
        val payload = "{\"event\":\"exit_training\"}"
        Log.d(TAG_WSVM, "sendExitTraining")
        sendMessage(payload)
    }

    private fun sendUserBindIfLoggedIn() {
        val uid = runCatching { UserSession.uid }.getOrNull() ?: return

        // 构造 JSON：有 hwWeight 就带上；没有就只发 uid
        val obj = JSONObject().apply {
            put("event", "setUser")
            put("userId", uid)
            UserSession.hwWeight?.let { hw ->
                // 可选：避免无效值，如果 <= 0 则不发送
                if (hw > 0f) put("hwWeight", hw)
            }
        }

        webSocket?.send(obj.toString())
    }


    fun disconnect() {
        webSocket?.close(1000, null)
        webSocket = null
    }

    override fun onCleared() {
        super.onCleared()
        scanner.stopScan()
        disconnect()
    }

    fun clearEvent() {
        _wsEvents.value = null
    }
}
