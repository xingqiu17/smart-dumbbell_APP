package com.example.dumb_app.feature.workout

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dumb_app.core.connectivity.wifi.WifiScanViewModel
import com.example.dumb_app.core.connectivity.wifi.WifiScanViewModel.WsEvent
import com.example.dumb_app.core.model.Log.LogDayCreateReq
import com.example.dumb_app.core.model.Log.LogItemCreateReq
import com.example.dumb_app.core.model.Log.LogWorkCreateReq
import com.example.dumb_app.core.network.NetworkModule
import com.example.dumb_app.core.repository.TrainingRepository
import com.example.dumb_app.core.util.TrainingSession
import com.example.dumb_app.core.util.UserSession
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    navController: NavController,
    wifiVm: WifiScanViewModel
) {
    // ① Repo & VM
    val repo = remember { TrainingRepository(NetworkModule.apiService) }
    val vm: TrainingViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(clz: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TrainingViewModel(repo) as T
            }
        }
    )

    // ② UI 状态
    val uiState by vm.uiState.collectAsState()
    val cachedSid = TrainingSession.sessionId
    val initialType = TrainingSession.items.firstOrNull()?.type ?: 0
    val initialTarget = TrainingSession.firstTargetReps ?: 0

    var showFinishDialog by remember { mutableStateOf(false) }
    var finishTriggered by remember { mutableStateOf(false) }

    // ③ 动态展示状态
    var exerciseName by remember { mutableStateOf(exerciseNameOf(initialType)) }
    var current by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(initialTarget) }
    var score by remember { mutableStateOf<Double?>(null) }
    var mediaUri by remember { mutableStateOf(mediaUriFor(initialType)) }

    // ====== 本地累积：待写入日志的结构 ======
    data class WorkRec(val acOrder: Int, val score: Int)
    data class PendingItem(
        val type: Int,
        val tOrder: Int,
        val tWeight: Float,
        val num: Int,
        val works: SnapshotStateList<WorkRec> = mutableStateListOf()
    ) {
        val avgScore: Int
            get() = if (works.isEmpty()) 0 else works.map { it.score }.average().roundToInt()
    }

    // 基于当前计划初始化待写入项（一个计划项 = 一条 LogItem）
    val pendingItems = remember {
        mutableStateListOf<PendingItem>().apply {
            TrainingSession.items.forEachIndexed { idx, it ->
                val order = try {
                    // 优先用计划的 tOrder；没有则用下标兜底
                    @Suppress("UNNECESSARY_SAFE_CALL")
                    (it.tOrder ?: (idx + 1))
                } catch (_: Throwable) {
                    idx + 1
                }
                val weight = try {
                    it.tWeight
                } catch (_: Throwable) {
                    // 若计划没有重量，兜底 0f
                    0f
                }
                add(
                    PendingItem(
                        type = it.type,
                        tOrder = order,
                        tWeight = weight,
                        num = it.number
                    )
                )
            }
        }
    }

    // 找到应记录的 PendingItem：优先未达标的；否则取同 type 的第一条
    fun findActiveItemIndexByType(type: Int): Int? {
        val i1 = pendingItems.indexOfFirst { it.type == type && it.works.size < it.num }
        if (i1 >= 0) return i1
        val i2 = pendingItems.indexOfFirst { it.type == type }
        return if (i2 >= 0) i2 else null
    }

    // 进入时重置展示状态
    LaunchedEffect(cachedSid) {
        current = 0
        total = initialTarget
        exerciseName = exerciseNameOf(initialType)
        mediaUri = mediaUriFor(initialType)
        score = null
    }

    // ④ 监听硬件上报：更新 UI + 累积 works
    val wsEvent by wifiVm.wsEvents.collectAsState(initial = null)
    LaunchedEffect(wsEvent) {
        when (val e = wsEvent) {
            is WsEvent.ExerciseData -> {
                exerciseName = exerciseNameOf(e.exercise)
                current = e.rep
                TrainingSession.items.firstOrNull { it.type == e.exercise }?.let { total = it.number }
                score = e.score
                mediaUri = mediaUriFor(e.exercise)

                // 累积写入 works
                findActiveItemIndexByType(e.exercise)?.let { idx ->
                    val item = pendingItems[idx]
                    val k = e.rep.coerceAtLeast(1)     // acOrder 从 1 开始
                    val s = (e.score?.roundToInt()) ?: 0

                    // 若上报跨越多次，补齐中间空位
                    while (item.works.size < k - 1) {
                        item.works.add(WorkRec(acOrder = item.works.size + 1, score = 0))
                    }
                    if (item.works.size >= k) {
                        item.works[k - 1] = WorkRec(acOrder = k, score = s)
                    } else {
                        item.works.add(WorkRec(acOrder = k, score = s))
                    }
                }
            }
            else -> Unit
        }
    }

    // 达标只弹一次
    LaunchedEffect(current, total) {
        if (!finishTriggered && total > 0 && current >= total) {
            finishTriggered = true
            showFinishDialog = true
        }
    }

    BackHandler {
        TrainingSession.clear()
        navController.popBackStack()
    }

    // ⑤ 无会话兜底
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center
            ) {
                Text("未找到训练会话，请从“开始运动”中选择计划后进入。")
            }
        }
        return
    }

    val headerTitle = when {
        exerciseName.isNotBlank() -> exerciseName
        !TrainingSession.title.isNullOrBlank() -> TrainingSession.title!!
        else -> "训练中"
    }

    // ⑥ UI
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text(headerTitle) },
                navigationIcon = {
                    IconButton(
                        onClick = {
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 中部媒体
            if (!mediaUri.isNullOrEmpty()) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(mediaUri))
                            setOnPreparedListener { it.isLooping = true; start() }
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
                    Text("此处显示动作演示（GIF/视频）", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(24.dp))

            // 底部进度 & 评分
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${current.coerceAtLeast(0)}/${total.coerceAtLeast(0)}",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = score?.let { String.format("%.1f", it) } ?: "--",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.End
                )
            }
        }

        if (showFinishDialog) {
            AlertDialog(
                onDismissRequest = { /* 禁止点外面关闭，保持显式确认 */ },
                title = { Text("恭喜你，训练完成") },
                text = { Text("本组训练已完成。") },
                confirmButton = {
                    TextButton(onClick = {
                        showFinishDialog = false

                        // 组装创建请求体并提交保存
                        val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                        val createReq = LogDayCreateReq(
                            userId = UserSession.uid,
                            date = dateStr,
                            items = pendingItems.map { pi ->
                                LogItemCreateReq(
                                    type = pi.type,
                                    tOrder = pi.tOrder,
                                    tWeight = pi.tWeight,
                                    num = pi.num,
                                    avgScore = pi.avgScore,
                                    works = if (pi.works.isEmpty()) null
                                    else pi.works.map { w ->
                                        LogWorkCreateReq(acOrder = w.acOrder, score = w.score)
                                    }
                                )
                            }
                        )
                        vm.saveLog(createReq)   // 交给 VM 写库（你稍后给我 VM，我来对上）

                        // 原有完成流程
                        vm.markPlanComplete()
                        TrainingSession.clear()
                        val popped = navController.popBackStack("workout", inclusive = false)
                        if (!popped) navController.popBackStack()
                    }) {
                        Text("确定")
                    }
                }
            )
        }
    }
}

/** 简单动作名映射：按你的项目实际调整 */
private fun exerciseNameOf(type: Int): String = when (type) {
    1 -> "哑铃弯举"
    2 -> "肩推"
    3 -> "卧推"
    4 -> "划船"
    5 -> "深蹲"
    else -> "动作$type"
}

/** 如需视频/GIF，可在这里返回对应 uri；没有就返回 null */
private fun mediaUriFor(type: Int): String? = null
