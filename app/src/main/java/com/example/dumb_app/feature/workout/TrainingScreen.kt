package com.example.dumb_app.feature.workout

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dumb_app.core.connectivity.wifi.WifiScanViewModel
import com.example.dumb_app.core.connectivity.wifi.WifiScanViewModel.WsEvent
import com.example.dumb_app.core.util.TrainingSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    navController: NavController,
    wifiVm: WifiScanViewModel
) {
    // 1) 从 TrainingSession 读取已保存的计划（静态）
    val cachedSid = TrainingSession.sessionId
    val initialType = TrainingSession.items.firstOrNull()?.type ?: 0
    val initialTarget = TrainingSession.firstTargetReps ?: 0

    var showFinishDialog by remember { mutableStateOf(false) }
    var finishTriggered by remember { mutableStateOf(false) }

    // 2) 本地状态（动态）：当前动作名、次数、评分、媒体
    var exerciseName by remember { mutableStateOf(exerciseNameOf(initialType)) }
    var current by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(initialTarget) }
    var score by remember { mutableStateOf<Double?>(null) }
    var mediaUri by remember { mutableStateOf(mediaUriFor(initialType)) }

    // 3) 进入时做一次初始化（避免返回重组后丢失）
    LaunchedEffect(cachedSid) {
        current = 0
        total = initialTarget
        exerciseName = exerciseNameOf(initialType)
        mediaUri = mediaUriFor(initialType)
        score = null
    }

    // 4) 监听硬件回传 "rep_data"（动态更新：动作、次数、评分）
    val wsEvent by wifiVm.wsEvents.collectAsState(initial = null)
    LaunchedEffect(wsEvent) {
        when (val e = wsEvent) {
            is WsEvent.ExerciseData -> {
                exerciseName = exerciseNameOf(e.exercise)
                current = e.rep
                // 若计划中能找到该动作的目标次数，则更新 total
                TrainingSession.items.firstOrNull { it.type == e.exercise }?.let { total = it.number }
                score = e.score
                mediaUri = mediaUriFor(e.exercise)
            }
            else -> Unit
        }
    }

    // 当达到目标次数时，仅触发一次弹窗
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

    // 5) 无会话兜底
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

    // 6) UI：顶部标题；中部媒体；底部左进度/右评分
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
            // 中部媒体（有 mp4 就播；否则占位）
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

            // 底部：左侧进度，右侧评分
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
                        TrainingSession.clear()
                        // 返回“主界面”。优先精确回退到 workout；失败则普通返回
                        val popped = navController.popBackStack("workout", inclusive = false)
                        if (!popped) {
                            navController.popBackStack()
                        }
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
