package com.example.dumb_app.feature.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dumb_app.core.connectivity.wifi.WifiScanViewModel
import com.example.dumb_app.feature.record.PlanUiState
import com.example.dumb_app.feature.record.RecordViewModel
import com.example.dumb_app.core.model.Plan.PlanDayDto
import com.example.dumb_app.core.model.Plan.PlanItemDto
import com.example.dumb_app.core.util.TrainingSession
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    nav: NavController,
    vm: RecordViewModel = viewModel(),
    wifiVm: WifiScanViewModel = viewModel()
) {
    // 自动重连尝试
    LaunchedEffect(Unit) {
        wifiVm.getLastHost()?.let { host ->
            wifiVm.connectToDevice(host)
        }
    }

    // 构造下发给硬件的 JSON
    fun buildStartTrainingJson(items: List<PlanItemDto>): String {
        return JSONObject().apply {
            put("event", "start_training")
            put("sets", items.size)
            put("items", JSONArray().also { arr ->
                items.forEach { it ->
                    arr.put(JSONObject().apply {
                        put("type", it.type)
                        put("reps", it.number)
                        put("weight", it.tWeight)
                    })
                }
            })
        }.toString()
    }


    // 监听 WS 连接状态
    val wsEvent by wifiVm.wsEvents.collectAsState()

    // 连接状态文字
    val connStatus = when (wsEvent) {
        WifiScanViewModel.WsEvent.Connected       -> "已连接设备"
        WifiScanViewModel.WsEvent.Paired          -> "配对完成"
        WifiScanViewModel.WsEvent.PairRequested   -> "等待配对确认"
        is WifiScanViewModel.WsEvent.Error        -> "连接失败"
        else                                      -> "未连接设备"
    }

    // 记录模块状态
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    var showDetail by remember { mutableStateOf(false) }
    var currentItems by remember { mutableStateOf<List<PlanItemDto>>(emptyList()) }
    val today = LocalDate.now()
    LaunchedEffect(showSheet) {
        if (showSheet) vm.loadPlans(today.toString())
    }
    val planState by vm.planState.collectAsState()
    val sessions: List<PlanDayDto> =
        (planState as? PlanUiState.Success)
            ?.sessions
            ?.filter { !it.session.complete }          // 只留 complete == false
            ?.sortedBy { it.session.sessionId }
            ?: emptyList()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { nav.navigate("wifiConnect") }) {
                                Icon(Icons.Default.Add, contentDescription = "添加设备")
                            }
                            Text(
                                connStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                )
            },
            contentWindowInsets = WindowInsets(0)
        ) { inner ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(bottom = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .size(220.dp)
                        .shadow(8.dp, CircleShape),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { showSheet = true }
                ) {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .rotate(135f),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "开始运动",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            if (showSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSheet = false },
                    sheetState = sheetState,
                    dragHandle = null,
                    modifier = Modifier.fillMaxHeight(0.75f)
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            "选择训练计划",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))

                        when (planState) {
                            PlanUiState.Loading -> {
                                LinearProgressIndicator(Modifier.fillMaxWidth())
                            }
                            is PlanUiState.Error -> {
                                Text(
                                    text = (planState as PlanUiState.Error).msg,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                            PlanUiState.Empty -> {
                                Text(
                                    "今日暂无未完成的训练计划",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                )
                            }
                            is PlanUiState.Success -> {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    itemsIndexed(sessions) { idx, day ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    currentItems = day.items
                                                    showDetail = true
                                                }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("计划 ${idx + 1}", style = MaterialTheme.typography.bodyLarge)
                                                Text("共 ${day.items.size} 组动作", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Button(onClick = {
                                                showSheet = false

                                                // A) 保存当前选择
                                                val sid = day.session.sessionId ?: return@Button
                                                TrainingSession.update(
                                                    sessionId = sid,
                                                    items = day.items,
                                                    title = "计划 ${idx + 1}"
                                                )

                                                // B) 下发 JSON 给设备
                                                val payload = buildStartTrainingJson(day.items)
                                                wifiVm.sendMessage(payload)

                                                // C) 跳转到训练界面（无参路由）
                                                nav.navigate("training")
                                            }) {
                                                Text("开始训练")
                                            }

                                        }
                                        Divider()
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        // **始终显示的“创建训练计划”按钮**
                        Button(
                            onClick = {
                                showSheet = false
                                nav.navigate("createPlan")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("创建训练计划")
                        }
                    }
                }
            }

            if (showDetail) {
                AlertDialog(
                    onDismissRequest = { showDetail = false },
                    title = { Text("训练计划详情") },
                    text = {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            itemsIndexed(currentItems) { idx, item ->
                                Text(
                                    "${idx + 1}. 类型${item.type} × ${item.number}次，配重${item.tWeight}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showDetail = false }) {
                            Text("关闭")
                        }
                    }
                )
            }
        }
    }
}
