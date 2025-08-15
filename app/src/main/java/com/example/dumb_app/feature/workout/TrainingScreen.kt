// app/src/main/java/com/example/dumb_app/feature/workout/TrainingScreen.kt
package com.example.dumb_app.feature.workout

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.example.dumb_app.core.connectivity.wifi.WifiScanViewModel
import com.example.dumb_app.core.connectivity.wifi.WifiScanViewModel.WsEvent
import com.example.dumb_app.core.network.NetworkModule
import com.example.dumb_app.core.repository.TrainingRepository
import com.example.dumb_app.core.util.TrainingSession
import com.example.dumb_app.core.util.UserSession
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val TAG_TS = "TrainingScreen"
private const val TRAINING_ROUTE = "training"

// 仅为避免枚举初始化告警而保留的 Saver
private enum class FinishStage { NONE }
private val FinishStageSaver: Saver<FinishStage, String> = Saver(
    save = { it.name },
    restore = { FinishStage.valueOf(it) }
)

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

    // —— 基础信息 —— //
    val cachedSid = TrainingSession.sessionId
    val items = TrainingSession.items
    val totalSets = items.size
    Log.d(TAG_TS, "compose start: totalSets=$totalSets, cachedSid=$cachedSid")

    // 首次进入时，用 Session 初始化 VM
    LaunchedEffect(TrainingSession.sessionId) {
        vm.initFromSession()
    }

    // 从 VM 读取各组累计（跨导航不丢）
    val pendingItems by vm.pendingItems.collectAsState()

    // —— 局部 UI 状态 —— //
    var currentSetIndex by rememberSaveable { mutableStateOf(0) }
    var finishTriggered by rememberSaveable { mutableStateOf(false) }
    var inRest by rememberSaveable { mutableStateOf(false) }
    var startedThisSet by rememberSaveable { mutableStateOf(false) }

    // 去重键：包含 (type, rep, setIndex)
    var lastEventKey by rememberSaveable { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    // 组首计数闸门
    var mustSeeFirstRep by rememberSaveable { mutableStateOf(true) }

    // 弹窗相关
    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    var hasSubmitted by rememberSaveable { mutableStateOf(false) }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    val currentItem = items.getOrNull(currentSetIndex)
    val initialType = currentItem?.type ?: 0
    val initialTarget = currentItem?.number ?: 0

    var exerciseName by remember { mutableStateOf(exerciseNameOf(initialType)) }
    var currentRep   by remember { mutableStateOf(0) }
    var totalRep     by remember { mutableStateOf(initialTarget) }
    var score        by remember { mutableStateOf<Double?>(null) }

    // 固定获取“训练页” BackStackEntry
    val trainingEntryState = remember { mutableStateOf<NavBackStackEntry?>(null) }
    LaunchedEffect(navController) {
        runCatching {
            val entry = navController.getBackStackEntry(TRAINING_ROUTE)
            trainingEntryState.value = entry
            Log.d(TAG_TS, "got trainingEntry=$entry for route=$TRAINING_ROUTE")
        }.onFailure {
            Log.e(TAG_TS, "getBackStackEntry($TRAINING_ROUTE) failed: ${it.message}", it)
            trainingEntryState.value = null
        }
    }
    val trainingHandle = trainingEntryState.value?.savedStateHandle

    // 恢复等待状态
    LaunchedEffect(trainingHandle) {
        val awaiting = trainingHandle?.get<Boolean>("awaitingNextSet") == true
        Log.d(TAG_TS, "check awaitingNextSet at enter: $awaiting")
        if (awaiting) inRest = true
        trainingHandle?.remove<Boolean>("awaitingNextSet")
    }

    // 订阅 advanceSet
    DisposableEffect(trainingHandle) {
        if (trainingHandle == null) return@DisposableEffect onDispose { }
        val liveData = trainingHandle.getLiveData<Boolean>("advanceSet")
        val observer = androidx.lifecycle.Observer<Boolean> { goNext ->
            Log.d(TAG_TS, "observer advanceSet=$goNext (currentSetIndex=$currentSetIndex)")
            if (goNext == true) {
                mustSeeFirstRep = true
                lastEventKey    = null
                val next = (currentSetIndex + 1).coerceAtMost(totalSets - 1)
                Log.d(TAG_TS, "advance to next set: $currentSetIndex -> $next")
                currentSetIndex = next
                trainingHandle.remove<Boolean>("advanceSet")
                trainingHandle.remove<Boolean>("awaitingNextSet")
            }
        }
        liveData.observeForever(observer)
        onDispose { liveData.removeObserver(observer) }
    }

    // 切组重置
    LaunchedEffect(currentSetIndex) {
        Log.d(TAG_TS, "onSetChanged -> setIndex=$currentSetIndex, type=${currentItem?.type}, num=${currentItem?.number}")
        finishTriggered = false
        startedThisSet  = false
        mustSeeFirstRep = true
        lastEventKey    = null
        exerciseName = exerciseNameOf(currentItem?.type ?: 0)
        totalRep     = currentItem?.number ?: 0
        currentRep   = 0
        score        = null
        inRest       = false
    }

    // —— 监听硬件上报 —— //
    val wsEvent by wifiVm.wsEvents.collectAsState(initial = null)
    LaunchedEffect(wsEvent) {
        if (wsEvent == null) return@LaunchedEffect
        when (val e = wsEvent) {

            is WsEvent.TrainingExited -> {
                Log.d(TAG_TS, "Device requested to exit training.")
                vm.savePartialTraining(UserSession.uid)
                TrainingSession.clear()
                navController.popBackStack("workout", false)
                wifiVm.clearEvent()
            }

            is WsEvent.ExerciseData -> {
                if (inRest) {
                    Log.d(TAG_TS, "wsEvent ignored: inRest=true, event=$wsEvent")
                    return@LaunchedEffect
                }
                val cur = currentItem ?: return@LaunchedEffect
                if (e.exercise != cur.type) return@LaunchedEffect

                // 去重：包含 setIndex
                val eventKey = Triple(e.exercise, e.rep, currentSetIndex)
                if (lastEventKey == eventKey) {
                    Log.d(TAG_TS, "ignore duplicated wsEvent: $eventKey")
                    return@LaunchedEffect
                }

                // 组首闸门：你的硬件每组 rep 从 1 开始
                if (mustSeeFirstRep) {
                    if (e.rep != 1) {
                        Log.d(TAG_TS, "ignore until first rep of current set (rep=${e.rep})")
                        lastEventKey = eventKey
                        return@LaunchedEffect
                    }
                    mustSeeFirstRep = false
                    startedThisSet  = true
                }

                // 交给 VM 累计
                vm.applyExerciseData(
                    setIndex     = currentSetIndex,
                    expectedType = cur.type,
                    rep          = e.rep,
                    score        = e.score
                )

                // —— 同步 UI 展示 —— //
                val k = e.rep.coerceAtMost(cur.number)
                currentRep = k
                score      = e.score
                totalRep   = cur.number
                exerciseName = exerciseNameOf(e.exercise)

                lastEventKey = eventKey
                val worksInSet = pendingItems.getOrNull(currentSetIndex)?.works?.size ?: 0
                Log.d(TAG_TS, "UI update -> currentRep=$currentRep/$totalRep, set=$currentSetIndex, works_in_set=$worksInSet")
            }
            else -> Log.d(TAG_TS, "other wsEvent=$wsEvent")
        }
    }

    // 完成判定
    LaunchedEffect(currentRep, totalRep) {
        if (inRest || !startedThisSet) {
            Log.d(TAG_TS, "skip completion: inRest=$inRest, startedThisSet=$startedThisSet, cur=$currentRep, total=$totalRep")
            return@LaunchedEffect
        }
        if (!finishTriggered && totalRep > 0 && currentRep >= totalRep) {
            finishTriggered = true
            Log.d(TAG_TS, "SET COMPLETED -> setIndex=$currentSetIndex, totalSets=$totalSets")
            if (currentSetIndex < totalSets - 1) {
                val restTime = items.getOrNull(currentSetIndex)?.rest ?: 60
                inRest = true
                trainingHandle?.set("awaitingNextSet", true)
                navController.navigate("rest/$restTime")
            } else {
                Log.d(TAG_TS, "ALL SETS DONE -> autosave & show done dialog")
                if (!hasSubmitted) {
                    hasSubmitted = true
                    vm.fillMissingZeros()
                    val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                    val req = vm.buildLogDayCreateReq(
                        userId = UserSession.uid,
                        date   = dateStr
                    )
                    // vm.saveLog(req)
                    // vm.markPlanComplete()
                }
                pendingItems.forEachIndexed { i, pi ->
                    Log.d(TAG_TS, "ready to save [set=${i+1}] -> type=${pi.type}, num=${pi.num}, works=${pi.works.size}, avg=${pi.avgScore}")
                }
                showFinishDialog = true
            }
        }
    }

    // 返回键处理
    BackHandler {
        if (showFinishDialog) {
            showFinishDialog = false
            return@BackHandler
        }
        Log.d(TAG_TS, "Back pressed -> clear session & popBackStack")
        TrainingSession.clear()
        navController.popBackStack(TRAINING_ROUTE, false)
    }

    if (cachedSid == null) {
        Log.d(TAG_TS, "no session -> fallback UI")
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center
            ) {
                Text("未找到训练会话，请重新选择计划。")
            }
        }
        return
    }

    // ====== UI：顶部统计 + 评分 + 可滚动“已完成明细”，底部固定“退出训练” ======
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text(TrainingSession.title ?: "训练中") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (showFinishDialog) {
                                showFinishDialog = false
                                return@IconButton
                            }
                            Log.d(TAG_TS, "toolbar back -> clear session & popBackStack")
                            TrainingSession.clear()
                            navController.popBackStack()
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        // ✅ 退出训练固定在最底部
        bottomBar = {
            Surface(tonalElevation = 2.dp, shadowElevation = 8.dp) {
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(
                        onClick = {
                            Log.d(TAG_TS, "exit training clicked")
                            showExitDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("退出训练", color = Color.White)
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
            // 顶部：当前动作名称
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            // —— 统计卡片：已完成 / 预计 —— //
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 已完成
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "已完成",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$currentRep",
                            style = MaterialTheme.typography.displaySmall
                        )
                    }

                    // 竖向分隔
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(44.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    )

                    // 预计
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "预计",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$totalRep",
                            style = MaterialTheme.typography.displaySmall
                        )
                    }
                }
            }

            // ✅ 评分放在卡片下方，居中显示
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

            Spacer(Modifier.height(16.dp))

            // —— 可滚动“已完成动作及评分”明细 —— //
            Text(
                text = "已完成明细",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))

            // 从 VM 汇总所有已完成的（到当前为止）
            val completedList = remember(pendingItems) {
                pendingItems.flatMapIndexed { setIdx, pi ->
                    pi.works.mapIndexed { repIdx, sc ->
                        CompletedRow(
                            setIndex = setIdx + 1,
                            repIndex = repIdx + 1,
                            typeName = exerciseNameOf(pi.type),
                            scoreStr = sc?.let { String.format("%.1f", it) } ?: "--"
                        )
                    }
                }
            }

            // 让列表占据剩余空间，不足时可滚动；底部按钮固定不动
            if (completedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无已完成记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    // 额外底部内边距，避免被底部栏遮挡（即便内容很长也能完整滚动到）
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(completedList) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${row.typeName} · 第${row.setIndex}组 第${row.repIndex}次",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = row.scoreStr,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Divider()
                    }
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("退出训练") },
            text = { Text("确定要退出训练吗？未完成动作将记为0分。") },
            confirmButton = {
                TextButton(onClick = {
                    Log.d(TAG_TS, "exit confirmed -> save and exit")
                    vm.savePartialTraining(UserSession.uid)
                    wifiVm.sendExitTraining()
                    TrainingSession.clear()
                    navController.popBackStack("workout", false)
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("取消") }
            }
        )
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("训练完成") },
            text = { Text("恭喜你完成训练！") },
            confirmButton = {
                TextButton(
                    onClick = {
                        TrainingSession.clear()
                        navController.popBackStack("workout", false)
                    }
                ) { Text("确定") }
            }
        )
    }
}

/** 简单动作名映射 */
private fun exerciseNameOf(type: Int): String = when (type) {
    1 -> "哑铃弯举"
    2 -> "肩推"
    3 -> "卧推"
    4 -> "划船"
    5 -> "深蹲"
    else -> "动作$type"
}

/** 展示行数据结构（仅用于 UI） */
private data class CompletedRow(
    val setIndex: Int,
    val repIndex: Int,
    val typeName: String,
    val scoreStr: String
)
