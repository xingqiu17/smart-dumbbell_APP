// app/src/main/java/com/example/dumb_app/feature/record/TrainingRecordDetailScreen.kt
package com.example.dumb_app.feature.record

import android.graphics.Paint
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dumb_app.core.model.Log.LogDayDto
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// === 新增：AI 接入 ===
import com.example.dumb_app.core.ai.AiPromptBuilder
import com.example.dumb_app.core.ai.AiReportRepository
import com.example.dumb_app.core.ai.ZhipuGlmRepository
import com.example.dumb_app.core.ai.AiConfig

// 明细评分内部结构（增加 performance）
private data class ActionRecord(val score: Int, val performance: Int?)

// 组视图模型（带上真实次数与配重）
private data class GroupRecord(
    val groupNumber: Int,
    val groupId: Int?,
    val type: Int,
    val actionName: String,
    val actualReps: Int,
    val weightKg: Float,
    val avgScoreFromItem: Int,
    val actionRecords: List<ActionRecord>
) {
    val averageScore: Float =
        if (actionRecords.isEmpty()) avgScoreFromItem.toFloat()
        else actionRecords.map { it.score }.average().toFloat()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingRecordDetailScreen(
    navController: NavController
) {
    val currentEntry by navController.currentBackStackEntryAsState()
    val recordEntry = remember(currentEntry) { navController.getBackStackEntry("record") }
    val vm: RecordViewModel = viewModel(recordEntry)

    val selectedLog by vm.selectedLog.collectAsState()
    val log: LogDayDto? = selectedLog

    val worksMap by vm.worksMap.collectAsState()

    LaunchedEffect(log?.session?.recordId) {
        val groupIds = log?.items?.mapNotNull { it.groupId } ?: emptyList()
        if (groupIds.isNotEmpty()) vm.loadWorksFor(groupIds)
    }

    // 颜色：轴线更克制，折线中性，点颜色跟“完成情况”映射
    val axisColor = MaterialTheme.colorScheme.outline
    val chartLineColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f)

    // —— 主题色先取出来（普通 Color），供普通函数使用 —— //
    val perfColorStandard = Color(0xFF2E7D32) // 标准 -> 绿色
    val perfColorSmall    = MaterialTheme.colorScheme.tertiary
    val perfColorCheat    = MaterialTheme.colorScheme.error
    val perfColorDefault  = MaterialTheme.colorScheme.onSurfaceVariant

    // ========= AI 报告状态 =========
    data class AiUiState(
        val running: Boolean = false,
        val text: String = "",
        val error: String? = null,
        val started: Boolean = false // 控制按钮隐藏
    )
    var aiState by remember { mutableStateOf(AiUiState()) }
    val scope = rememberCoroutineScope()
    // 使用真实实现（直接在 App 里写 key）
    val aiRepo: AiReportRepository = remember {
        ZhipuGlmRepository(apiKey = AiConfig.ZHIPU_API_KEY)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("训练详情") },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.popBackStack()
                            vm.clearSelectedLog()
                        }) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                    }
                )
            }
        ) { inner ->
            if (log == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner),
                    contentAlignment = Alignment.Center
                ) { Text("无训练记录详情", style = MaterialTheme.typography.bodyLarge) }
            } else {
                // 实际次数 = 明细条数（有则用），否则回退 item.num
                val groups = log.items
                    .sortedBy { it.tOrder }
                    .mapIndexed { idx, item ->
                        val works = item.groupId?.let { worksMap[it] }.orEmpty()
                        val actualReps = if (works.isNotEmpty()) works.size else item.num
                        GroupRecord(
                            groupNumber       = idx + 1,
                            groupId           = item.groupId,
                            type              = item.type,
                            actionName        = exerciseNameOf(item.type),
                            actualReps        = actualReps,
                            weightKg          = item.tWeight,
                            avgScoreFromItem  = item.avgScore,
                            actionRecords     = works.map { ActionRecord(score = it.score, performance = it.performance) }
                        )
                    }

                // 顶部总览：总消耗 + 首组配重
                val sessionTotalKcal = remember(groups) {
                    groups.sumOf {
                        EnergyEstimator.estimateSet(it.weightKg, max(it.actualReps, 0)).kcalTotal.toDouble()
                    }.toFloat()
                }
                val sessionFirstWeight = remember(groups) { groups.firstOrNull()?.weightKg ?: 0f }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "本次训练共消耗了 ${sessionTotalKcal.format1()} kcal",
                            style = MaterialTheme.typography.bodyMedium ,
                            fontSize = 20.sp
                        )
                    }
                    item {
                        Text(
                            "本次训练配重为： ${sessionFirstWeight.format1()} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 20.sp
                        )
                    }

                    // 分组卡片
                    items(groups) { group ->
                        GroupCard(
                            group = group,
                            axisColor = axisColor,
                            chartLineColor = chartLineColor,
                            perfColorStandard = perfColorStandard,
                            perfColorSmall = perfColorSmall,
                            perfColorCheat = perfColorCheat,
                            perfColorDefault = perfColorDefault
                        )
                    }

                    // ====== 底部：AI 实时生成分析报告 ======
                    // 1) 生成按钮（未开始时显示；一旦点击，隐藏）
                    if (!aiState.started) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    aiState = AiUiState(running = true, started = true)
                                    val prompt = AiPromptBuilder.buildTrainingAnalysisPrompt(log)
                                    scope.launch {
                                        try {
                                            aiRepo.streamTrainingAnalysis(
                                                prompt = prompt,
                                                model = "glm-4.5",
                                                thinkingType = "enabled"
                                            ).collectLatest { delta ->
                                                aiState = aiState.copy(
                                                    text = aiState.text + delta,
                                                    running = true,
                                                    error = null
                                                )
                                            }
                                            aiState = aiState.copy(running = false)
                                        } catch (e: Throwable) {
                                            aiState = aiState.copy(
                                                running = false,
                                                error = e.message ?: "生成失败"
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) { Text("AI 实时生成分析报告", color = Color.White) }
                        }
                    }

                    // 2) 生成中的进度条
                    if (aiState.started) {
                        item {
                            if (aiState.running) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }

                    // 3) 报告卡片（流式内容追加）
                    if (aiState.started) {
                        item {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "AI 分析报告",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    if (aiState.text.isNotBlank()) {
                                        Text(aiState.text, style = MaterialTheme.typography.bodyMedium)
                                    } else if (aiState.running) {
                                        Text("正在生成中…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    aiState.error?.let { err ->
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "生成失败：$err",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                            TextButton(onClick = {
                                                aiState = AiUiState(running = true, started = true)
                                                val prompt = AiPromptBuilder.buildTrainingAnalysisPrompt(log)
                                                scope.launch {
                                                    try {
                                                        aiRepo.streamTrainingAnalysis(
                                                            prompt = prompt,
                                                            model = "glm-4.5",
                                                            thinkingType = "enabled"
                                                        ).collectLatest { delta ->
                                                            aiState = aiState.copy(
                                                                text = aiState.text + delta,
                                                                running = true,
                                                                error = null
                                                            )
                                                        }
                                                        aiState = aiState.copy(running = false)
                                                    } catch (e: Throwable) {
                                                        aiState = aiState.copy(
                                                            running = false,
                                                            error = e.message ?: "生成失败"
                                                        )
                                                    }
                                                }
                                            }) { Text("重试") }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

/* === 子组件：每组卡片（保持你原实现） === */
@Composable
private fun GroupCard(
    group: GroupRecord,
    axisColor: Color,
    chartLineColor: Color,
    perfColorStandard: Color,
    perfColorSmall: Color,
    perfColorCheat: Color,
    perfColorDefault: Color
) {
    var expanded by remember { mutableStateOf(false) }

    // 每组估算
    val calc = remember(group) {
        EnergyEstimator.estimateSet(
            weightKg = group.weightKg,
            reps = max(group.actualReps, 0)
        )
    }

    // 完成情况文本
    val statusTexts = remember(group.actionRecords, group.type) {
        group.actionRecords.map { performanceNameOf(group.type, it.performance) }
    }
    // 完成情况颜色
    val statusColors = remember(group.actionRecords, group.type, perfColorSmall, perfColorCheat, perfColorDefault) {
        group.actionRecords.map { ar ->
            colorForPerformanceRaw(
                performance = ar.performance,
                cStandard = perfColorStandard,
                cSmall = perfColorSmall,
                cCheat = perfColorCheat,
                cDefault = perfColorDefault
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "第${group.groupNumber}组：${group.actionName}  ${group.actualReps} 次",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "收起" else "展开"
                    )
                }
            }
            Text(
                text = "卡路里：≈ ${calc.kcalTotal.format1()} kcal（%1RM≈${calc.percent1Rm.roundToInt()}%）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 2.dp, bottom = 6.dp)
            )
            Text(
                text = "平均分：" + (if (group.actionRecords.isEmpty())
                    group.avgScoreFromItem.toString()
                else
                    group.averageScore.toInt().toString()),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 4.dp, bottom = 8.dp)
            )

            if (expanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Text("序号", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("完成情况", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                    Text("评分", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    group.actionRecords.forEachIndexed { i, rec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text(
                                statusTexts.getOrNull(i) ?: "--",
                                style = MaterialTheme.typography.bodyLarge,
                                color = statusColors.getOrNull(i) ?: perfColorDefault,
                                modifier = Modifier.weight(1.2f),
                                textAlign = TextAlign.Center
                            )
                            Text("${rec.score}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                ScoreChart(
                    scores = group.actionRecords.map { it.score.toFloat() },
                    perfs = group.actionRecords.map { it.performance },
                    axisColor = axisColor,
                    chartLineColor = chartLineColor,
                    perfColorStandard = perfColorStandard,
                    perfColorSmall = perfColorSmall,
                    perfColorCheat = perfColorCheat,
                    perfColorDefault = perfColorDefault
                )
            }
        }
    }
}

@Composable
private fun ScoreChart(
    scores: List<Float>,
    perfs: List<Int?>,
    axisColor: Color,
    chartLineColor: Color,
    perfColorStandard: Color,
    perfColorSmall: Color,
    perfColorCheat: Color,
    perfColorDefault: Color
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current
    val hitRadiusPx = with(density) { 16.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(start = 32.dp, top = 16.dp, end = 16.dp, bottom = 32.dp)
            .pointerInput(scores) {
                detectTapGestures { offset ->
                    val count = scores.size
                    if (count == 0 || canvasSize.width == 0) {
                        selectedIndex = null
                        return@detectTapGestures
                    }
                    val w = canvasSize.width.toFloat()
                    val h = canvasSize.height.toFloat()
                    val dx = if (count > 1) w / (count - 1) else w
                    val approxIndex = ((offset.x / dx).roundToInt()).coerceIn(0, count - 1)
                    val px = approxIndex * dx
                    val py = h * (1f - scores[approxIndex] / 100f)
                    val dist = kotlin.math.sqrt((offset.x - px) * (offset.x - px) + (offset.y - py) * (offset.y - py))
                    selectedIndex = if (dist <= hitRadiusPx) approxIndex else null
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size -> canvasSize = size }
        ) {
            val count = scores.size
            if (count >= 1) {
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                val dx = if (count > 1) w / (count - 1) else w
                val tick = 4.dp.toPx()
                val labelSize = 12.dp.toPx()
                val paint = Paint().apply {
                    color = AndroidColor.BLACK
                    textSize = labelSize
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }

                drawLine(axisColor, Offset(0f, h), Offset(w, h), 2f)
                drawLine(axisColor, Offset(0f, 0f), Offset(0f, h), 2f)

                scores.forEachIndexed { i, _ ->
                    val x = i * dx
                    drawLine(axisColor, Offset(x, h), Offset(x, h - tick), 1f)
                    drawContext.canvas.nativeCanvas.drawText(
                        "${i + 1}", x, h + labelSize + 4.dp.toPx(), paint
                    )
                }
                listOf(0f, 25f, 50f, 75f, 100f).forEach { v ->
                    val y = h * (1f - v / 100f)
                    drawLine(axisColor, Offset(0f, y), Offset(tick, y), 1f)
                    drawContext.canvas.nativeCanvas.drawText(
                        "${v.toInt()}",
                        -labelSize,
                        y + labelSize / 2,
                        paint.apply { textAlign = Paint.Align.RIGHT }
                    )
                }

                if (count >= 2) {
                    val path = Path().apply {
                        scores.forEachIndexed { i, s ->
                            val x = i * dx
                            val y = h * (1f - s / 100f)
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                    }
                    drawPath(
                        path,
                        chartLineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                scores.forEachIndexed { i, s ->
                    val x = i * dx
                    val y = h * (1f - s / 100f)
                    val pointColor = colorForPerformanceRaw(
                        performance = perfs.getOrNull(i),
                        cStandard = perfColorStandard,
                        cSmall = perfColorSmall,
                        cCheat = perfColorCheat,
                        cDefault = perfColorDefault
                    )
                    drawCircle(pointColor, radius = 5.dp.toPx(), center = Offset(x, y))
                }

                selectedIndex?.let { sel ->
                    val sx = sel * dx
                    val sy = h * (1f - scores[sel] / 100f)
                    drawLine(
                        axisColor.copy(alpha = 0.5f),
                        Offset(sx, 0f),
                        Offset(sx, h),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawLine(
                        axisColor.copy(alpha = 0.5f),
                        Offset(0f, sy),
                        Offset(w, sy),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawCircle(
                        color = chartLineColor,
                        radius = 8.dp.toPx(),
                        center = Offset(sx, sy),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

/** 动作名映射 */
private fun exerciseNameOf(type: Int): String = when (type) {
    1 -> "哑铃弯举"
    2 -> "侧平举"
    3 -> "卧推"
    4 -> "划船"
    5 -> "深蹲"
    else -> "动作$type"
}

/** 完成情况：按动作类型 + performance 值映射（1/2 已定义） */
private fun performanceNameOf(type: Int, performance: Int?): String {
    if (performance == null) return "--"
    return when (type) {
        1 -> when (performance) {
            0 -> "标准"
            1 -> "幅度偏小"
            2 -> "借力"
            else -> "--"
        }
        2 -> when (performance) {
            0 -> "标准"
            1 -> "肩内旋代偿"
            2 -> "躯干代偿"
            3 -> "下落过快"
            else -> "--"
        }
        else -> "--"
    }
}

/** 非 Composable 的颜色选择函数 */
private fun colorForPerformanceRaw(
    performance: Int?,
    cStandard: Color,
    cSmall: Color,
    cCheat: Color,
    cDefault: Color
): Color = when (performance) {
    0 -> cStandard
    1 -> cSmall
    2, 3 -> cCheat
    else -> cDefault
}

/* ====================== 估算器：统一用“重量×次数” ====================== */
private object EnergyEstimator {
    private const val TEMPO_SEC_PER_REP: Float = 3f  // 3 秒/次

    data class EstimationResult(
        val kcalTotal: Float,
        val percent1Rm: Float
    )

    fun estimateSet(
        weightKg: Float,
        reps: Int,
        tempoSecPerRep: Float = TEMPO_SEC_PER_REP
    ): EstimationResult {
        if (weightKg <= 0f || reps <= 0) return EstimationResult(kcalTotal = 0f, percent1Rm = 0f)
        val oneRm = estimate1RmEpley(weightKg, reps)
        val pct1Rm = if (oneRm > 0f) (weightKg / oneRm * 100f) else 0f
        val rateKcalPerMin = energyRateKcalPerMin(pct1Rm)
        val workMin = reps * tempoSecPerRep / 60f
        val kcal = rateKcalPerMin * workMin
        return EstimationResult(kcalTotal = kcal, percent1Rm = pct1Rm)
    }

    fun estimate1RmEpley(weightKg: Float, reps: Int): Float {
        return 10 * (1f + reps / 30f)
    }

    fun energyRateKcalPerMin(percent1Rm: Float): Float {
        val p = percent1Rm.coerceIn(20f, 80f)
        return 1.716f + 0.085f * p
    }
}

/* 小工具 */
private fun Float.format1(): String = String.format("%.1f", this)
