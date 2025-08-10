package com.example.dumb_app.feature.workout

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.WebSocket
import com.example.dumb_app.core.connectivity.wifi.WifiScanViewModel

private const val SERVICE_TYPE = "_smartdumbbell._tcp"
private const val TAG = "mDNS"

data class DiscoveredDevice(
    val serviceName: String,
    val host: String,
    val port: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiConnectScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 注入 ViewModel（包含 connectToDevice 和 wsEvents）
    val wifiVm: WifiScanViewModel = viewModel()
    val wsEvent by wifiVm.wsEvents.collectAsState()

    // 权限申请
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
        allGranted = perms.values.all { it }
        if (!allGranted) {
            Toast.makeText(context, "需要开启权限才能搜索设备", Toast.LENGTH_SHORT).show()
        }
    }

    // mDNS 发现
    val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    val devices = remember { mutableStateListOf<DiscoveredDevice>() }
    val resolveListener = remember {
        object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.e(TAG, "Resolve failed: $errorCode")
            }
            override fun onServiceResolved(info: NsdServiceInfo) {
                val addr = info.host.hostAddress ?: return
                val dev = DiscoveredDevice(info.serviceName, addr, info.port)
                if (devices.none { it.serviceName == dev.serviceName }) {
                    devices += dev
                }
            }
        }
    }
    var discoveryListener by remember { mutableStateOf<NsdManager.DiscoveryListener?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("连接设备") },
                navigationIcon = {
                    IconButton(onClick = {
                        discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
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
            // 连接状态提示
            wsEvent?.let { evt ->
                val msg = when (evt) {
                    WifiScanViewModel.WsEvent.PairRequested -> "请在设备端确认配对"
                    WifiScanViewModel.WsEvent.Paired -> "配对成功！"
                    WifiScanViewModel.WsEvent.Connected -> "已自动连接设备"
                    is WifiScanViewModel.WsEvent.Error -> "连接错误：${evt.msg}"
                    is WifiScanViewModel.WsEvent.Data -> "收到数据：${evt.payload}"
                    is WifiScanViewModel.WsEvent.TrainingStarted -> "开始训练"
                    is WifiScanViewModel.WsEvent.RestSkipped -> "跳过休息"
                    is WifiScanViewModel.WsEvent.TrainingExited -> "退出训练"
                    is WifiScanViewModel.WsEvent.ExerciseData -> {
                        // 具体显示运动数据
                        "运动编号: ${evt.exercise}\n次数: ${evt.rep}\n得分: ${evt.score}"
                    }
                    null -> ""
                }
                if (msg.isNotEmpty()) {
                    Text(msg, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                }
            }

            // 提示文字
            Text(
                when {
                    !allGranted      -> "请先授予发现权限"
                    devices.isEmpty() -> "点击“开始搜索”以发现设备"
                    else              -> "发现到以下设备："
                },
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))

            // 搜索按钮
            OutlinedButton(onClick = {
                if (!allGranted) {
                    permissionLauncher.launch(requiredPermissions)
                } else {
                    discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
                    val listener = object : NsdManager.DiscoveryListener {
                        override fun onDiscoveryStarted(s: String) {}
                        override fun onServiceFound(service: NsdServiceInfo) {
                            if (service.serviceType.startsWith(SERVICE_TYPE)) {
                                nsdManager.resolveService(service, resolveListener)
                            }
                        }
                        override fun onServiceLost(service: NsdServiceInfo) {
                            devices.removeAll { it.serviceName == service.serviceName }
                        }
                        override fun onDiscoveryStopped(s: String) { discoveryListener = null }
                        override fun onStartDiscoveryFailed(s: String, errorCode: Int) {
                            Toast.makeText(context, "启动 mDNS 失败", Toast.LENGTH_SHORT).show()
                            discoveryListener = null
                        }
                        override fun onStopDiscoveryFailed(s: String, errorCode: Int) {
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
                        Toast.makeText(context, "启动 mDNS 失败: ${it.message}", Toast.LENGTH_LONG).show()
                        discoveryListener = null
                    }
                }
            }) { Text("开始搜索") }

            Spacer(Modifier.height(24.dp))

            // 设备列表
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
                                    // 用户点击后调用 ViewModel 连接
                                    wifiVm.connectToDevice(dev.host)
                                },
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(dev.serviceName, style = MaterialTheme.typography.bodyLarge)
                                    Text("${dev.host}:${dev.port}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
