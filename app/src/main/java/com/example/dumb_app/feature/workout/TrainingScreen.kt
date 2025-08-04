// app/src/main/java/com/example/dumb_app/feature/workout/TrainingScreen.kt
package com.example.dumb_app.feature.workout

import android.net.Uri
import android.util.Log
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import kotlin.math.roundToInt

private const val TAG_TS = "TrainingScreen"
private const val TRAINING_ROUTE = "training"

// 仅为避免枚举初始化告警而保留的 Saver（此版本已不再使用阶段流转）
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

    // 首次进入时，用 Session 初始化 VM 里的 pendingItems
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
    // 组首计数闸门：必须先看到本组 rep==1 才允许后续计数与判完成
    var mustSeeFirstRep by rememberSaveable { mutableStateOf(true) }

    // 完成提示弹窗
    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    // 确保后台保存只触发一次
    var hasSubmitted by rememberSaveable { mutableStateOf(false) }

    val currentItem = items.getOrNull(currentSetIndex)
    val initialType = currentItem?.type ?: 0
    val initialTarget = currentItem?.number ?: 0

    var exerciseName by remember { mutableStateOf(exerciseNameOf(initialType)) }
    var currentRep   by remember { mutableStateOf(0) }
    var totalRep     by remember { mutableStateOf(initialTarget) }
    var score        by remember { mutableStateOf<Double?>(null) }
    var mediaUri     by remember { mutableStateOf(mediaUriFor(initialType)) }

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
        mediaUri     = mediaUriFor(currentItem?.type ?: 0)
        currentRep   = 0
        score        = null
        inRest       = false
    }

    // —— 监听硬件上报：按“当前组”写入（交给 VM） —— //
    val wsEvent by wifiVm.wsEvents.collectAsState(initial = null)
    LaunchedEffect(wsEvent) {
        if (wsEvent == null) return@LaunchedEffect
        if (inRest) {
            Log.d(TAG_TS, "wsEvent ignored: inRest=true, event=$wsEvent")
            return@LaunchedEffect
        }

        when (val e = wsEvent) {
            is WsEvent.ExerciseData -> {
                val cur = currentItem ?: return@LaunchedEffect
                if (e.exercise != cur.type) return@LaunchedEffect

                // 去重：包含 setIndex
                val eventKey = Triple(e.exercise, e.rep, currentSetIndex)
                if (lastEventKey == eventKey) {
                    Log.d(TAG_TS, "ignore duplicated wsEvent: $eventKey")
                    return@LaunchedEffect
                }

                // 组首闸门：你的硬件每组 rep 从 1 开始，上闸只放行 rep==1
                if (mustSeeFirstRep) {
                    if (e.rep != 1) {
                        Log.d(TAG_TS, "ignore until first rep of current set (rep=${e.rep})")
                        lastEventKey = eventKey
                        return@LaunchedEffect
                    }
                    mustSeeFirstRep = false
                    startedThisSet  = true
                }

                // 交给 VM 累计写入（跨导航不丢）
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
                mediaUri     = mediaUriFor(e.exercise)

                lastEventKey = eventKey
                val worksInSet = pendingItems.getOrNull(currentSetIndex)?.works?.size ?: 0
                Log.d(TAG_TS, "UI update -> currentRep=$currentRep/$totalRep, set=$currentSetIndex, works_in_set=$worksInSet")
            }
            else -> Log.d(TAG_TS, "other wsEvent=$wsEvent")
        }
    }

    // 完成判定：最后一组 -> 后台自动保存 & 弹出“恭喜完成”
    LaunchedEffect(currentRep, totalRep) {
        if (inRest || !startedThisSet) {
            Log.d(TAG_TS, "skip completion: inRest=$inRest, startedThisSet=$startedThisSet, cur=$currentRep, total=$totalRep")
            return@LaunchedEffect
        }
        if (!finishTriggered && totalRep > 0 && currentRep >= totalRep) {
            finishTriggered = true
            Log.d(TAG_TS, "SET COMPLETED -> setIndex=$currentSetIndex, totalSets=$totalSets")
            if (currentSetIndex < totalSets - 1) {
                inRest = true
                trainingHandle?.set("awaitingNextSet", true)
                navController.navigate("rest")
            } else {
                Log.d(TAG_TS, "ALL SETS DONE -> autosave & show done dialog")
                // —— 仅触发一次的后台保存（无 UI 干预，不显示错误）——
                if (!hasSubmitted) {
                    hasSubmitted = true
                    // （可选）补齐每组未做到的次数为 0，保证 works.size == num
                    vm.fillMissingZeros()
                    val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                    val req = vm.buildLogDayCreateReq(
                        userId = UserSession.uid,
                        date   = dateStr
                    )
                    vm.saveLog(req)
                    // 尝试标记计划完成（失败也不提示，不影响退出）
                    vm.markPlanComplete()
                }

                // 调试输出：逐组汇总（来自 VM）
                pendingItems.forEachIndexed { i, pi ->
                    Log.d(TAG_TS, "ready to save [set=${i+1}] -> type=${pi.type}, num=${pi.num}, works=${pi.works.size}, avg=${pi.avgScore}")
                }

                showFinishDialog = true
            }
        }
    }

    // 返回键：弹窗打开先收起；否则直接返回训练路由上一层
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
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!mediaUri.isNullOrEmpty()) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(mediaUri))
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                start()
                            }
                        }
                    },
                    update = { vv -> if (!vv.isPlaying) vv.start() }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("此处显示动作演示", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$currentRep/$totalRep", style = MaterialTheme.typography.headlineLarge)
                Text(score?.let { String.format("%.1f", it) } ?: "--",
                    style = MaterialTheme.typography.headlineLarge)
            }
        }
    }

    // —— 训练完成弹窗（仅提示 + 确定） —— //
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { /* 完成提示不支持点外部取消 */ },
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

/** 媒体 URI，如无则返回 null */
private fun mediaUriFor(type: Int): String? = null
