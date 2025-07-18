package com.example.dumb_app.feature.workout

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import java.net.DatagramSocket
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.SocketTimeoutException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocketListener
import okhttp3.WebSocket
import okhttp3.Response




/** mDNS 服务类型（与设备端保持一致） */
private const val SERVICE_TYPE = "_smartdumbbell._tcp"
private const val TAG = "mDNS"

/** 保存解析后的设备信息 */
data class DiscoveredDevice(
    val serviceName: String,
    val host: InetAddress,
    val port: Int
)




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiConnectScreen(nav: NavController) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun connectWs(ip: String, port: Int) {
        val client = OkHttpClient()
        val req = Request.Builder()
            .url("ws://$ip:$port/ws")         // ★ 路径 /ws
            .build()

        client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, r: Response) {
                Log.i(TAG, "WS 已连接")
                ws.send("HELLO")
            }
            override fun onMessage(ws: WebSocket, text: String) {
                Log.i(TAG, "收到: $text")
            }
            override fun onFailure(ws: WebSocket, t: Throwable, r: Response?) {
                Log.e(TAG, "WS 失败", t)
            }
            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WS 关闭 $code $reason")
            }
        })
    }

    /* ---------- 权限申请 ---------- */
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }
    var allGranted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        Log.d(TAG, "Permission result: $perms")
        allGranted = perms.values.all { it }
        if (!allGranted) {
            Toast.makeText(context, "需要开启权限才能搜索设备", Toast.LENGTH_SHORT).show()
        }
    }

    /* ---------- mDNS 相关 ---------- */
    val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    val devices = remember { mutableStateListOf<DiscoveredDevice>() }

    /* 解析回调 */
    val resolveListener = remember {
        object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.e(TAG, "Resolve failed: $errorCode")
            }
            override fun onServiceResolved(info: NsdServiceInfo) {
                Log.i(TAG, "Service resolved: ${info.serviceName} -> ${info.host}:${info.port}")
                val dev = DiscoveredDevice(info.serviceName, info.host, info.port)
                if (devices.none { it.serviceName == dev.serviceName }) devices += dev
            }
        }
    }

    /* 发现回调 */

    var discoveryListener by remember { mutableStateOf<NsdManager.DiscoveryListener?>(null) }

    /* 组件销毁时停止发现 */
    DisposableEffect(Unit) {
        onDispose {
            discoveryListener?.let {
                runCatching { nsdManager.stopServiceDiscovery(it) }
            }
        }
    }

    /* --------------------- UI --------------------- */
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("连接设备 ") },
                navigationIcon = {
                    IconButton(onClick = {
                        discoveryListener?.let {
                            runCatching { nsdManager.stopServiceDiscovery(it) }
                        }
                        nav.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            /* 提示文字 */
            Text(
                when {
                    !allGranted      -> "请先授予发现权限"
                    devices.isEmpty() -> "点击“开始搜索”以发现设备"
                    else              -> "发现到以下设备："
                },
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))

            /* 开始搜索按钮 */
            OutlinedButton(onClick = {
                Log.d(TAG, "Search button clicked")

                if (!allGranted) {
                    Log.d(TAG, "Requesting permissions…")
                    permissionLauncher.launch(requiredPermissions)
                } else {
                    Log.d(TAG, "Starting discovery (new listener)…")
                    // …停止旧发现
                    discoveryListener?.let {
                        runCatching { nsdManager.stopServiceDiscovery(it) }
                    }

                    val listener = object : NsdManager.DiscoveryListener {
                        override fun onDiscoveryStarted(s: String) {
                            Log.i(TAG, "Discovery started for $s")
                        }
                        override fun onServiceFound(service: NsdServiceInfo) {
                            Log.i(TAG, "Service found: ${service.serviceName} (${service.serviceType})")
                            if (service.serviceType.startsWith(SERVICE_TYPE)) {   // ← ★ 修改
                                nsdManager.resolveService(service, resolveListener)
                            }
                        }

                        override fun onServiceLost(service: NsdServiceInfo) {
                            Log.i(TAG, "Service lost: ${service.serviceName}")
                            devices.removeAll { it.serviceName == service.serviceName }
                        }
                        override fun onDiscoveryStopped(s: String) {
                            Log.i(TAG, "Discovery stopped")
                            discoveryListener = null
                        }
                        override fun onStartDiscoveryFailed(s: String, errorCode: Int) {
                            Log.e(TAG, "Start discovery failed: $errorCode")
                            discoveryListener = null
                        }
                        override fun onStopDiscoveryFailed(s: String, errorCode: Int) {
                            Log.e(TAG, "Stop discovery failed: $errorCode")
                            discoveryListener = null
                        }
                    }

                    discoveryListener = listener
                    runCatching {
                        nsdManager.discoverServices(
                            SERVICE_TYPE,
                            NsdManager.PROTOCOL_DNS_SD,
                            listener
                        )
                    }.onFailure {
                        Log.e(TAG, "discoverServices() exception", it)
                        Toast.makeText(context, "启动 mDNS 失败: ${it.message}", Toast.LENGTH_LONG).show()
                        discoveryListener = null
                    }
                }
            }) { Text("开始搜索") }


            Spacer(Modifier.height(24.dp))

            /* 设备列表 */
            if (allGranted && devices.isNotEmpty()) {
                LazyColumn(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(devices) { dev ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { connectWs(dev.host.hostAddress, dev.port) }
                                },
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(dev.serviceName, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "${dev.host.hostAddress}:${dev.port}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ---------- 设备连接函数 ---------- */
/**
 * 向设备发一个 UDP “HELLO”，并等待回应。
 * 这里只是演示；实际协议可换成 TCP / HTTP / WebSocket 等。
 */
private fun connectToDevice(ip: String, port: Int) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val socket = DatagramSocket()
            socket.soTimeout = 1_000       // 1 秒超时

            // 发送 HELLO
            val data = "HELLO".toByteArray()
            socket.send(
                DatagramPacket(
                    data, data.size,
                    InetAddress.getByName(ip), port
                )
            )

            // 等待回应
            val buf = ByteArray(32)
            val resp = DatagramPacket(buf, buf.size)
            socket.receive(resp)           // 若 1 秒无回应会抛 SocketTimeoutException

            Log.i(TAG, "已连接设备：${resp.address.hostAddress}:${resp.port} -> ${String(resp.data, 0, resp.length)}")
            socket.close()

            // TODO：这里可切回主线程，更新 UI 或导航到“设备详情”页面
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "设备无回应")
        } catch (e: Exception) {
            Log.e(TAG, "连接失败", e)
        }
    }
}
