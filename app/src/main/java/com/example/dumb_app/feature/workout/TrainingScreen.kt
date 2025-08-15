// app/src/main/java/com/example/dumb_app/feature/workout/TrainingScreen.kt
package com.example.dumb_app.feature.workout

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dumb_app.core.connectivity.wifi.WifiScanViewModel
import com.example.dumb_app.core.connectivity.wifi.WifiScanViewModel.WsEvent
import com.example.dumb_app.core.network.NetworkModule
import com.example.dumb_app.core.repository.TrainingRepository
import com.example.dumb_app.core.util.TrainingSession
import com.example.dumb_app.core.util.UserSession
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val TAG_TS = "TrainingScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    navController: NavController,
    wifiVm: WifiScanViewModel
) {
    val repo = remember { TrainingRepository(NetworkModule.apiService) }
    val vm: TrainingViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(clz: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TrainingViewModel(repo) as T
            }
        }
    )

    // —— 基础 —— //
    val cachedSid = TrainingSession.sessionId
    val items = TrainingSession.items
    val totalSets = items.size
    Log.d(TAG_TS, "compose start: totalSets=$totalSets, cachedSid=$cachedSid")

    LaunchedEffect(TrainingSession.sessionId) { vm.initFromSession() }
    val pendingItems by vm.pendingItems.collectAsState()

    // —— UI/业务状态 —— //
    var currentSetIndex by rememberSaveable { mutableStateOf(0) }
    var startedThisSet by rememberSaveable { mutableStateOf(false) }
    var finishTriggered by rememberSaveable { mutableStateOf(false) }

    // 休息内嵌倒计时
    var inRest by rememberSaveable { mutableStateOf(false) }
    var restTotal by rememberSaveable { mutableStateOf(0) }
    var secondsRemaining by rememberSaveable { mutableStateOf(0) }

    // 训练已全部完成，但选择“返回”留在页面
    var trainingCompleted by rememberSaveable { mutableStateOf(false) }

    // 去重/组首闸门
    var lastEventKey by rememberSaveable { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var mustSeeFirstRep by rememberSaveable { mutableStateOf(true) }

    // 弹窗
    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    var hasSubmitted by rememberSaveable { mutableStateOf(false) }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    val currentItem = items.getOrNull(currentSetIndex)
    val initialType = currentItem?.type ?: 0
    val initialTarget = currentItem?.number ?: 0

    var exerciseName by rememberSaveable { mutableStateOf(exerciseNameOf(initialType)) }
    var currentRep by rememberSaveable { mutableStateOf(0) }
    var totalRep by rememberSaveable { mutableStateOf(initialTarget) }
    var score by rememberSaveable { mutableStateOf<Double?>(null) }
    var currentExLabel by rememberSaveable { mutableStateOf<Int?>(null) }

    // —— 硬件事件 —— //
    val wsEvent by wifiVm.wsEvents.collectAsState(initial = null)
    LaunchedEffect(wsEvent) {
        when (val e = wsEvent) {
            is WsEvent.TrainingExited -> {
                Log.d(TAG_TS, "Device requested to exit training.")
                vm.savePartialTraining(UserSession.uid)
                TrainingSession.clear()
                navController.popBackStack("workout", false)
                wifiVm.clearEvent()
            }
            is WifiScanViewModel.WsEvent.RestSkipped -> {
                Log.d(TAG_TS, "Received skip rest from device.")
                inRest = false
                advanceToNextSet(
                    totalSets = totalSets,
                    curIdx = currentSetIndex,
                    onIdx = { currentSetIndex = it },
                    onResetSetState = {
                        mustSeeFirstRep = true
                        lastEventKey = null
                        startedThisSet = false
                    },
                    onExitAllDone = {
                        handleAllDone(vm) { showFinishDialog = true; hasSubmitted = true }
                    }
                )
                wifiVm.clearEvent()
            }
            is WsEvent.ExerciseData -> {
                if (inRest || trainingCompleted) return@LaunchedEffect
                val cur = currentItem ?: return@LaunchedEffect
                if (e.exercise != cur.type) return@LaunchedEffect

                val eventKey = Triple(e.exercise, e.rep, currentSetIndex)
                if (lastEventKey == eventKey) return@LaunchedEffect

                if (mustSeeFirstRep) {
                    if (e.rep != 1) {
                        lastEventKey = eventKey
                        return@LaunchedEffect
                    }
                    mustSeeFirstRep = false
                    startedThisSet = true
                }

                vm.applyExerciseData(
                    setIndex = currentSetIndex,
                    expectedType = cur.type,
                    rep = e.rep,
                    score = e.score,
                    exLabel = e.exLabel
                )

                currentRep = e.rep.coerceAtMost(cur.number)
                score = e.score
                totalRep = cur.number
                exerciseName = exerciseNameOf(e.exercise)
                currentExLabel = e.exLabel

                lastEventKey = eventKey
            }
            else -> Unit
        }
    }

    // —— 切组重置 —— //
    LaunchedEffect(currentSetIndex) {
        finishTriggered = false
        startedThisSet = false
        mustSeeFirstRep = true
        lastEventKey = null
        exerciseName = exerciseNameOf(currentItem?.type ?: 0)
        totalRep = currentItem?.number ?: 0
        currentRep = 0
        score = null
        currentExLabel = null
        inRest = false
        trainingCompleted = false
    }

    // —— 完成判定 —— //
    LaunchedEffect(currentRep, totalRep) {
        if (inRest || !startedThisSet || trainingCompleted) return@LaunchedEffect
        if (!finishTriggered && totalRep > 0 && currentRep >= totalRep) {
            finishTriggered = true
            if (currentSetIndex < totalSets - 1) {
                // 启动内嵌休息
                val restTime = items.getOrNull(currentSetIndex)?.rest ?: 60
                restTotal = restTime
                secondsRemaining = restTime
                inRest = true
            } else {
                if (!hasSubmitted) {
                    handleAllDone(vm) { showFinishDialog = true; hasSubmitted = true }
                }
            }
        }
    }

    // —— 休息倒计时 —— //
    LaunchedEffect(inRest) {
        if (!inRest) return@LaunchedEffect
        while (inRest && secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
        }
        if (inRest && secondsRemaining <= 0) {
            advanceToNextSet(
                totalSets = totalSets,
                curIdx = currentSetIndex,
                onIdx = { currentSetIndex = it },
                onResetSetState = {
                    mustSeeFirstRep = true
                    lastEventKey = null
                    startedThisSet = false
                },
                onExitAllDone = {
                    handleAllDone(vm) { showFinishDialog = true; hasSubmitted = true }
                }
            )
            inRest = false
        }
    }

    // —— 返回键 —— //
    BackHandler {
        if (showFinishDialog) {
            showFinishDialog = false
            trainingCompleted = true
            return@BackHandler
        }
        TrainingSession.clear()
        navController.popBackStack("training", false)
    }

    if (cachedSid == null) {
        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = { Text("训练中") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { inner ->
            Box(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center
            ) { Text("未找到训练会话，请重新选择计划。") }
        }
        return
    }

    // —— UI —— //
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text(if (inRest) "休息中" else (TrainingSession.title ?: "训练中")) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (showFinishDialog) {
                                showFinishDialog = false
                                trainingCompleted = true
                            } else {
                                TrainingSession.clear()
                                navController.popBackStack()
                            }
                        }
                    ) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp, shadowElevation = 8.dp) {
                Box(Modifier.fillMaxWidth().padding(16.dp)) {

                    // 明确是 BtnCfg，避免任何泛型/lambda 推断问题
                    val btn: BtnCfg = when {
                        trainingCompleted -> BtnCfg(
                            text = "完成训练",
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            TrainingSession.clear()
                            navController.popBackStack("workout", false)
                        }

                        inRest -> BtnCfg(
                            text = "跳过休息",
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            wifiVm.sendSkipRest()
                            inRest = false
                            advanceToNextSet(
                                totalSets = totalSets,
                                curIdx = currentSetIndex,
                                onIdx = { currentSetIndex = it },
                                onResetSetState = {
                                    mustSeeFirstRep = true
                                    lastEventKey = null
                                    startedThisSet = false
                                },
                                onExitAllDone = {
                                    handleAllDone(vm) { showFinishDialog = true; hasSubmitted = true }
                                }
                            )
                        }

                        else -> BtnCfg(
                            text = "退出训练",
                            color = Color.Red
                        ) {
                            showExitDialog = true
                        }
                    }

                    Button(
                        onClick = btn.onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = btn.color),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(btn.text, color = Color.White)
                    }
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val header = when {
                trainingCompleted -> "训练已完成"
                inRest -> "休息"
                else -> exerciseName
            }
            Text(
                text = header,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            if (inRest) {
                val progress by animateFloatAsState(
                    targetValue = if (restTotal > 0) secondsRemaining / restTotal.toFloat() else 0f,
                    animationSpec = tween(durationMillis = 400, easing = LinearEasing)
                )
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = progress,
                                strokeWidth = 8.dp,
                                modifier = Modifier.size(160.dp)
                            )
                            Text(
                                text = secondsRemaining.toString(),
                                style = MaterialTheme.typography.headlineLarge
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "休息倒计时",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // 统计卡片：已完成/预计
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("已完成", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$currentRep", style = MaterialTheme.typography.displaySmall)
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp).height(44.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        )
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("预计", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalRep", style = MaterialTheme.typography.displaySmall)
                        }
                    }
                }

                // 评分
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "评分：${score?.let { String.format("%.1f", it) } ?: "--"}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 明细表
            Text(
                text = "已完成明细（当前组）",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("个数", modifier = Modifier.weight(0.3f), style = MaterialTheme.typography.labelLarge)
                    Text("评分", modifier = Modifier.weight(0.35f), style = MaterialTheme.typography.labelLarge)
                    Text("完成状况", modifier = Modifier.weight(0.35f), style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(6.dp))

            val curPi = pendingItems.getOrNull(currentSetIndex)
            val curType = curPi?.type ?: 0
            val rows = curPi?.works?.map { w ->
                CompletedRow(
                    count = w.acOrder,
                    scoreText = String.format("%.1f", w.score.toDouble()),
                    labelName = labelNameOf(curType, w.exLabel),
                    exLabel = w.exLabel
                )
            } ?: emptyList()

            if (rows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) { Text("暂无已完成记录", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(rows) { r ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(r.count.toString(), modifier = Modifier.weight(0.3f), style = MaterialTheme.typography.bodyLarge)
                            Text(r.scoreText, modifier = Modifier.weight(0.35f), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                r.labelName,
                                modifier = Modifier.weight(0.35f),
                                style = MaterialTheme.typography.bodyLarge,
                                color = labelTextColor(r.exLabel)
                            )
                        }
                        Divider()
                    }
                }
            }
        }
    }

    // 退出训练对话框
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("退出训练") },
            text = { Text("确定要退出训练吗？未完成动作将记为0分。") },
            confirmButton = {
                TextButton(onClick = {
                    wifiVm.sendExitTraining()
                    TrainingSession.clear()
                    navController.popBackStack("workout", false)
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("取消") } }
        )
    }

    // 训练完成对话框（退出 / 返回）
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { /* 不可点击空白取消 */ },
            title = { Text("训练完成") },
            text = { Text("恭喜你完成训练！") },
            confirmButton = {
                TextButton(
                    onClick = {
                        TrainingSession.clear()
                        navController.popBackStack("workout", false)
                    }
                ) { Text("退出") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showFinishDialog = false
                        trainingCompleted = true
                    }
                ) { Text("返回") }
            }
        )
    }
}

// —— 公共逻辑 —— //
private fun advanceToNextSet(
    totalSets: Int,
    curIdx: Int,
    onIdx: (Int) -> Unit,
    onResetSetState: () -> Unit,
    onExitAllDone: () -> Unit
) {
    val next = curIdx + 1
    if (next < totalSets) {
        onResetSetState()
        onIdx(next)
    } else {
        onExitAllDone()
    }
}

private fun handleAllDone(
    vm: TrainingViewModel,
    showDialog: () -> Unit
) {
    vm.fillMissingZeros()
    val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
    val _req = vm.buildLogDayCreateReq(
        userId = UserSession.uid,
        date = dateStr
    )
    // vm.saveLog(_req)
    // vm.markPlanComplete()
    showDialog()
}

// —— 辅助映射/UI —— //
private fun exerciseNameOf(type: Int): String = when (type) {
    1 -> "哑铃弯举"
    2 -> "侧平举"
    3 -> "卧推"
    4 -> "划船"
    5 -> "深蹲"
    else -> "动作$type"
}

private fun labelNameOf(type: Int, exLabel: Int?): String {
    if (exLabel == null) return "--"
    return if (type == 1) {
        when (exLabel) {
            0 -> "标准"
            1 -> "幅度偏小"
            2 -> "借力"
            else -> "其他"
        }
    } else if (type == 2) {
        when (exLabel) {
            0 -> "标准"
            1 -> "肩内旋代偿"
            2 -> "躯干代偿"
            3 -> "下落过快"
            else -> "其他"
        }
    } else "--"
}

@Composable
private fun labelTextColor(exLabel: Int?): Color = when (exLabel) {
    0 -> MaterialTheme.colorScheme.primary
    1 -> MaterialTheme.colorScheme.tertiary
    2, 3 -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private data class CompletedRow(
    val count: Int,
    val scoreText: String,
    val labelName: String,
    val exLabel: Int?
)

private data class BtnCfg(
    val text: String,
    val color: Color,
    val onClick: () -> Unit
)
