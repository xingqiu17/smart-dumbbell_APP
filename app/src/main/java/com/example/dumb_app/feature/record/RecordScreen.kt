// app/src/main/java/com/example/dumb_app/feature/record/RecordScreen.kt

package com.example.dumb_app.feature.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*
import androidx.navigation.NavController
import java.time.temporal.TemporalAdjusters
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(navController: NavController) {
    var isWeekView by remember { mutableStateOf(true) }
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }

    // 示例：已训练日期列表
    val trainingDates = remember { listOf(today.minusDays(2), today) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 标题栏：年月 + 切换按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selectedDate.year} / ${selectedDate.monthValue}",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = { isWeekView = !isWeekView }) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "切换视图")
            }
        }

        // 周视图 or 月视图
        if (isWeekView) {
            val weekDates = generateWeekDates(selectedDate)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items(weekDates) { date ->
                    DateCell(
                        date = date,
                        isSelected = date == selectedDate,
                        isToday = date == today,
                        isTrained = trainingDates.contains(date)
                    ) { selectedDate = date }
                }
            }
        } else {
            // 月视图顶部星期标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DayOfWeek.values().forEach { dow ->
                    Text(
                        text = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            // 完整月网格
            val monthDates = generateMonthDates(selectedDate)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                monthDates.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),         // 保持格子正方形，可选
                                contentAlignment = Alignment.TopCenter
                            ) {
                                DateCell(
                                    date        = date,
                                    isSelected  = date == selectedDate,
                                    isToday     = date == today,
                                    isTrained   = trainingDates.contains(date),
                                    showWeekDay = false       // 只在头部显示星期
                                ) { selectedDate = date }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Divider()

        // 当日训练记录 列表
        Text(
            text = "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日 训练记录",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            textAlign = TextAlign.Center
        )
        LazyColumn {
            items(listOf("第一次训练", "第二次训练")) { item ->
                ListItem(
                    headlineContent = { Text(item) },
                    trailingContent = {
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                )
                Divider()
            }
        }
    }
}

@Composable
private fun DateCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isTrained: Boolean,
    showWeekDay: Boolean = true,
    onClick: () -> Unit
) {
    val now = LocalDate.now()
    val textColor = when {
        date.isBefore(now) -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        date.isAfter(now)  -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        else               -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        // 星期缩写（仅当 showWeekDay 为 true）
        if (showWeekDay) {
            Text(
                text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
            Spacer(Modifier.height(4.dp))
        }

        // 日期泡泡：只有这个圆包含背景，训练小点在它外面
        val bubbleBg = when {
            isToday    -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else       -> Color.Transparent
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(bubbleBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isToday) MaterialTheme.colorScheme.onPrimary else textColor
            )
        }

        // 训练日小圆点
        if (isTrained) {
            Spacer(Modifier.height(2.dp))
            Box(
                Modifier
                    .size(6.dp)
                    .background(Color(0xFF4CAF50), shape = CircleShape)
            )
        } else {
            Spacer(Modifier.height(8.dp))
        }
    }
}



private fun generateWeekDates(center: LocalDate): List<LocalDate> {
    val firstDayOfWeek = center.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0..6).map { firstDayOfWeek.plusDays(it.toLong()) }
}

private fun generateMonthDates(center: LocalDate): List<LocalDate> {
    val firstOfMonth = center.withDayOfMonth(1)
    val lastOfMonth  = center.withDayOfMonth(center.lengthOfMonth())
    // 从本月第一天的 周一（当日或之前）开始
    val start = firstOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    // 到本月最后一天的 周日（当日或之后）结束
    val end   = lastOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val days = ChronoUnit.DAYS.between(start, end).toInt() + 1
    return (0 until days).map { start.plusDays(it.toLong()) }
}
