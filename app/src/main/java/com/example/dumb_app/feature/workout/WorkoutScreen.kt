package com.example.dumb_app.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dumb_app.feature.record.RecordViewModel
import com.example.dumb_app.feature.record.PlanUiState
import com.example.dumb_app.core.model.PlanDayDto
import com.example.dumb_app.core.model.PlanItemDto
import androidx.compose.foundation.shape.CircleShape


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    nav: NavController,
    vm: RecordViewModel = viewModel()
) {
    // Bottom-Sheet 状态
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }

    // 详情对话框状态
    var showDetail by remember { mutableStateOf(false) }
    var currentItems by remember { mutableStateOf<List<PlanItemDto>>(emptyList()) }

    // 获取今天日期并拉取当日计划
    val today = java.time.LocalDate.now()
    LaunchedEffect(showSheet) {
        if (showSheet) {
            vm.loadPlans(today.toString())
        }
    }
    val uiState by vm.uiState.collectAsState()

    // 从 uiState 取出 sessions，按 sessionId 升序
    val sessions: List<PlanDayDto> = when (uiState) {
        is PlanUiState.Success -> (uiState as PlanUiState.Success).sessions
            .sortedBy { it.session.sessionId }
        else -> emptyList()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // 顶部 + 底部导航省略，保留“开始运动”按钮
        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { nav.navigate("wifiConnect") }) {
                            Icon(Icons.Default.Add, contentDescription = "添加设备")
                        }
                    }
                )
            },
            contentWindowInsets = WindowInsets(0)
        ) { inner ->
            Box(
                modifier = Modifier
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
                        modifier = Modifier.fillMaxSize(),
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
        }

        // 全屏 75% 高 Bottom-Sheet：列出当日各训练计划
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
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                ) {
                    Text(
                        "选择训练计划",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))

                    // 顶部 Loading / Error / Empty
                    when (uiState) {
                        PlanUiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        is PlanUiState.Error -> Text(
                            text = (uiState as PlanUiState.Error).msg,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                        PlanUiState.Empty -> Text(
                            "今日暂无训练计划",
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                        else -> {}
                    }

                    Spacer(Modifier.height(8.dp))

                    // 列表：计划 1、计划 2…
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(sessions) { idx, day ->
                            Row(
                                modifier = Modifier
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
                                    // TODO: 发送训练信息给设备
                                    showSheet = false
                                }) {
                                    Text("开始训练")
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        }

        // 小弹窗：展示选中计划的动作明细
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
