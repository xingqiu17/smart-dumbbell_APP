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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.net.InetAddress

// 用来保存解析后的设备信息
data class DiscoveredDevice(
    val serviceName: String,
    val host: InetAddress,
    val port: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiConnectScreen(
    nav: NavController
) {
    val context = LocalContext.current

    // 1. 权限申请（同前面，只写关键部分）
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
        allGranted = perms.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(context, "需要开启权限才能搜索设备", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. mDNS 发现相关
    val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    // 保存发现到的设备
    val devices = remember { mutableStateListOf<DiscoveredDevice>() }
    // 解析回调
    val resolveListener = remember {
        object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                // 解析失败可忽略或打印日志
            }
            override fun onServiceResolved(info: NsdServiceInfo) {
                // 解析成功后把设备加入列表（去重）
                val dev = DiscoveredDevice(
                    serviceName = info.serviceName,
                    host = info.host,
                    port = info.port
                )
                if (devices.none { it.serviceName == dev.serviceName }) {
                    devices += dev
                }
            }
        }
    }
    // 发现回调
    var discoveryListener by remember { mutableStateOf<NsdManager.DiscoveryListener?>(null) }
    val newDiscoveryListener = remember {
        object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) { /* 可显示正在搜索 */ }
            override fun onServiceFound(service: NsdServiceInfo) {
                // 只处理我们注册的服务类型
                if (service.serviceType == "_smartdumbbell._udp.local.") {
                    nsdManager.resolveService(service, resolveListener)
                }
            }
            override fun onServiceLost(service: NsdServiceInfo) {
                // 设备下线时移除
                devices.removeAll { it.serviceName == service.serviceName }
            }
            override fun onDiscoveryStopped(serviceType: String) { /* 可更新 UI */ }
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
        }
    }

    // 退出 Composable 时停止发现
    DisposableEffect(nsdManager) {
        onDispose {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("连接设备 (mDNS)") },
                navigationIcon = {
                    IconButton(onClick = {
                        // 停止发现后返回
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
            // 提示文字
            Text(
                text = when {
                    !allGranted -> "请先授予发现权限"
                    devices.isEmpty() -> "点击“开始搜索”以发现设备"
                    else -> "发现到以下设备："
                },
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))

            // 搜索按钮
            OutlinedButton(onClick = {
                if (!allGranted) {
                    permissionLauncher.launch(requiredPermissions)
                } else {
                    // 清空旧列表
                    devices.clear()
                    // 停止旧的发现
                    discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
                    // 开始新的 mDNS 发现
                    discoveryListener = newDiscoveryListener
                    nsdManager.discoverServices(
                        "_smartdumbbell._udp.local.",
                        NsdManager.PROTOCOL_DNS_SD,
                        newDiscoveryListener
                    )
                }
            }) {
                Text("开始搜索")
            }

            Spacer(Modifier.height(24.dp))

            // 列表展示
            if (allGranted && devices.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(devices) { dev ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // TODO: 点击后用 dev.host / dev.port 建立 TCP/HTTP/UDP 连接
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
