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
import androidx.navigation.NavController

// 单次动作完成记录，只记录评分
private data class ActionRecord(val score: Int)

// 一组动作记录，包含动作名称和所有完成次数的评分
private data class GroupRecord(
    val groupNumber: Int,
    val actionName: String,
    val actionRecords: List<ActionRecord>
) {
    val averageScore: Float = actionRecords.map { it.score }.average().toFloat()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingRecordDetailScreen(navController: NavController) {
    // 预设展示数据：三组示例
    val groups = remember {
        listOf(
            GroupRecord(1, "哑铃弯举",
                listOf(80, 82, 85, 90, 88, 92, 84, 86, 89, 91, 87, 83).map { ActionRecord(it) }
            ),
            GroupRecord(2, "深蹲",
                listOf(75, 78, 80, 82, 79, 81, 77, 76, 80, 83).map { ActionRecord(it) }
            ),
            GroupRecord(3, "俯卧撑",
                listOf(65, 68, 70, 72, 69, 71, 67, 66).map { ActionRecord(it) }
            )
        )
    }
    val primaryColor = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background     // ← 用主题背景色
    ) {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("训练详情") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { inner ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groups) { group ->
                    var expanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded },
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // 第一行：组信息 + 折叠/展开按钮
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "第${group.groupNumber}组：${group.actionName}  ${group.actionRecords.size} 次",
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

                            // 第二行：平均分
                            Text(
                                "平均分：${group.averageScore.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 32.dp, top = 4.dp, bottom = 8.dp)
                            )

                            if (expanded) {
                                // 表头：序号 / 评分
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp),
                                ) {
                                    Text(
                                        "序号",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "评分",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(Modifier.height(4.dp))

                                // 每一次动作及评分
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    group.actionRecords.forEachIndexed { index, record ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 32.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${index + 1}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                "${record.score}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))

                                // 折线图 + 坐标轴
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .padding(
                                            start = 32.dp,
                                            top = 16.dp,
                                            end = 16.dp,
                                            bottom = 32.dp
                                        )
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val scores = group.actionRecords.map { it.score.toFloat() }
                                        val count = scores.size
                                        if (count >= 2) {
                                            val w = size.width
                                            val h = size.height
                                            val dx = w / (count - 1)
                                            val tick = 4.dp.toPx()
                                            val labelSize = 12.dp.toPx()
                                            val paint = Paint().apply {
                                                color = AndroidColor.BLACK
                                                textSize = labelSize
                                                isAntiAlias = true
                                                textAlign = Paint.Align.CENTER
                                            }

                                            // 画坐标轴
                                            drawLine(primaryColor, Offset(0f, h), Offset(w, h), 2f)
                                            drawLine(
                                                primaryColor,
                                                Offset(0f, 0f),
                                                Offset(0f, h),
                                                2f
                                            )

                                            // x 轴刻度与标签
                                            scores.forEachIndexed { i, _ ->
                                                val x = i * dx
                                                drawLine(
                                                    primaryColor,
                                                    Offset(x, h),
                                                    Offset(x, h - tick),
                                                    1f
                                                )
                                                drawContext.canvas.nativeCanvas.drawText(
                                                    "${i + 1}",
                                                    x,
                                                    h + labelSize + 4.dp.toPx(),
                                                    paint
                                                )
                                            }

                                            // y 轴刻度与标签
                                            listOf(0f, 25f, 50f, 75f, 100f).forEach { v ->
                                                val y = h * (1f - v / 100f)
                                                drawLine(
                                                    primaryColor,
                                                    Offset(0f, y),
                                                    Offset(tick, y),
                                                    1f
                                                )
                                                drawContext.canvas.nativeCanvas.drawText(
                                                    "${v.toInt()}",
                                                    -labelSize,
                                                    y + labelSize / 2,
                                                    paint.apply { textAlign = Paint.Align.RIGHT }
                                                )
                                            }

                                            // 绘制折线
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
                                                style = Stroke(
                                                    width = 3.dp.toPx(),
                                                    cap = StrokeCap.Round
                                                )
                                            )

                                            // 绘制数据点
                                            scores.forEachIndexed { i, s ->
                                                val x = i * dx
                                                val y = h * (1f - s / 100f)
                                                drawCircle(
                                                    primaryColor,
                                                    radius = 4.dp.toPx(),
                                                    center = Offset(x, y)
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
        }
    }
}
