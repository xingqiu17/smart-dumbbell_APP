// app/src/main/java/com/example/dumb_app/feature/record/TrainingRecordDetailScreen.kt
package com.example.dumb_app.feature.record

import android.graphics.Paint
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.material3.CardDefaults
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dumb_app.core.model.Log.LogDayDto
import kotlin.math.max
import kotlin.math.roundToInt

// 明细评分内部结构（可为空）
private data class ActionRecord(val score: Int)

// 组视图模型（带上真实次数与配重）
private data class GroupRecord(
    val groupNumber: Int,
    val groupId: Int?,
    val type: Int,
    val actionName: String,
    val actualReps: Int,    // 实际完成次数：优先明细条数，否则 item.num
    val weightKg: Float,    // 配重
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
    val recordEntry = remember(currentEntry) {
        navController.getBackStackEntry("record")
    }
    val vm: RecordViewModel = viewModel(recordEntry)

    val selectedLog by vm.selectedLog.collectAsState()
    val log: LogDayDto? = selectedLog

    val worksMap by vm.worksMap.collectAsState()

    LaunchedEffect(log?.session?.recordId) {
        val groupIds = log?.items?.mapNotNull { it.groupId } ?: emptyList()
        if (groupIds.isNotEmpty()) vm.loadWorksFor(groupIds)
    }

    val primaryColor = MaterialTheme.colorScheme.primary

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
                // ①：实际次数 = 明细条数（有则用），否则回退 item.num
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
                            actionRecords     = works.map { ActionRecord(it.score) }
                        )
                    }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(groups) { group ->
                        var expanded by remember { mutableStateOf(false) }

                        // ②：无条件按当前重量与“实际次数”计算卡路里（重量或次数≤0则返回0）
                        val calc = remember(group) {
                            EnergyEstimator.estimateSet(
                                weightKg = group.weightKg,
                                reps = max(group.actualReps, 0)
                            )
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded },
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                // 标题行 —— 与计算一致，统一用 actualReps
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
                                // ③：卡路里 + %1RM（标题下一行）
                                Text(
                                    text = "卡路里：≈ ${calc.kcalTotal.format1()} kcal（%1RM≈${calc.percent1Rm.roundToInt()}%）",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 32.dp, top = 2.dp, bottom = 6.dp)
                                )

                                // 平均分（空明细回退 item.avgScore）
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
                                                Text("${rec.score}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(240.dp)
                                            .padding(start = 32.dp, top = 16.dp, end = 16.dp, bottom = 32.dp)
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val scores = group.actionRecords.map { it.score.toFloat() }
                                            val count = scores.size
                                            if (count >= 2) {
                                                val w = size.width
                                                val h = size.height
                                                val dx = if (count > 1) w / (count - 1) else w
                                                val tick = 4.dp.toPx()
                                                val labelSize = 12.dp.toPx()
                                                val paint = Paint().apply {
                                                    color = AndroidColor.BLACK
                                                    textSize = labelSize
                                                    isAntiAlias = true
                                                    textAlign = Paint.Align.CENTER
                                                }
                                                drawLine(primaryColor, Offset(0f, h), Offset(w, h), 2f)
                                                drawLine(primaryColor, Offset(0f, 0f), Offset(0f, h), 2f)
                                                scores.forEachIndexed { i, _ ->
                                                    val x = i * dx
                                                    drawLine(primaryColor, Offset(x, h), Offset(x, h - tick), 1f)
                                                    drawContext.canvas.nativeCanvas.drawText(
                                                        "${i + 1}", x, h + labelSize + 4.dp.toPx(), paint
                                                    )
                                                }
                                                listOf(0f, 25f, 50f, 75f, 100f).forEach { v ->
                                                    val y = h * (1f - v / 100f)
                                                    drawLine(primaryColor, Offset(0f, y), Offset(tick, y), 1f)
                                                    drawContext.canvas.nativeCanvas.drawText(
                                                        "${v.toInt()}",
                                                        -labelSize,
                                                        y + labelSize / 2,
                                                        paint.apply { textAlign = Paint.Align.RIGHT }
                                                    )
                                                }
                                                val path = Path().apply {
                                                    scores.forEachIndexed { i, s ->
                                                        val x = i * dx
                                                        val y = h * (1f - s / 100f)
                                                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                                                    }
                                                }
                                                drawPath(
                                                    path,
                                                    primaryColor,
                                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                                )
                                                scores.forEachIndexed { i, s ->
                                                    val x = i * dx
                                                    val y = h * (1f - s / 100f)
                                                    drawCircle(primaryColor, radius = 4.dp.toPx(), center = Offset(x, y))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 动作名映射 */
private fun exerciseNameOf(type: Int): String = when (type) {
    1 -> "哑铃弯举"
    2 -> "肩推"
    3 -> "卧推"
    4 -> "划船"
    5 -> "深蹲"
    else -> "动作$type"
}

/* ====================== 估算器：统一用“重量×次数” ====================== */
private object EnergyEstimator {
    private const val TEMPO_SEC_PER_REP: Float = 3f  // 3 秒/次，可按需调整

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
        val rateKcalPerMin = energyRateKcalPerMin(pct1Rm) // 中间量：kcal/min
        val workMin = reps * tempoSecPerRep / 60f
        val kcal = rateKcalPerMin * workMin
        return EstimationResult(kcalTotal = kcal, percent1Rm = pct1Rm)
    }

    // Epley：用同组的重量与实际次数估 1RM
    private fun estimate1RmEpley(weightKg: Float, reps: Int): Float {
        return weightKg * (1f + reps / 30f)
    }

    // 能量成本率（kcal/min）：用我们在弯举上验证过的 20–80%1RM 线性近似
    private fun energyRateKcalPerMin(percent1Rm: Float): Float {
        val p = percent1Rm.coerceIn(20f, 80f)
        return 1.716f + 0.085f * p
    }
}

/* 小工具 */
private fun Float.format1(): String = String.format("%.1f", this)
