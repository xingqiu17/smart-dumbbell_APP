// app/src/main/java/com/example/dumb_app/feature/record/RecordScreen.kt

package com.example.dumb_app.feature.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.layoutId
import androidx.navigation.NavController
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(navController: NavController) {
    var isWeekView by remember { mutableStateOf(true) }
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }

    // BottomSheet 状态
    val planSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPlanSheet by remember { mutableStateOf(false) }
    var currentPlan by remember { mutableStateOf("") }

    // 示例数据
    val trainingDates = remember { listOf(today.minusDays(2), today) }
    val trainingPlans = remember { listOf("计划 A", "计划 B") }
    val trainingRecords = remember { listOf("第一次训练", "第二次训练") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 日历头部
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${selectedDate.year} / ${selectedDate.monthValue}",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = { isWeekView = !isWeekView }) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "切换视图")
            }
        }

        // 周或月视图
        if (isWeekView) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items(generateWeekDates(selectedDate)) { date ->
                    DateCell(
                        date = date,
                        isSelected = date == selectedDate,
                        isToday = date == today,
                        isTrained = trainingDates.contains(date)
                    ) { selectedDate = date }
                }
            }
        } else {
            // 月视图星期标签
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
            // 月网格
            val monthDates = generateMonthDates(selectedDate)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                monthDates.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                DateCell(
                                    date = date,
                                    isSelected = date == selectedDate,
                                    isToday = date == today,
                                    isTrained = trainingDates.contains(date),
                                    showWeekDay = false
                                ) { selectedDate = date }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Divider()

        // ——— 训练计划 ———
        Text(
            text = "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日 训练计划",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            textAlign = TextAlign.Center
        )
        Column {
            trainingPlans.forEach { plan ->
                ListItem(
                    modifier = Modifier.clickable {
                        currentPlan = plan
                        showPlanSheet = true
                    },
                    headlineContent = { Text(plan) },
                    trailingContent = {
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                )
                Divider()
            }
        }

        Spacer(Modifier.height(16.dp))

        // ——— 训练记录 ———
        Text(
            text = "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日 训练记录",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            textAlign = TextAlign.Center
        )
        Column {
            trainingRecords.forEach { record ->
                ListItem(
                    modifier = Modifier.clickable {
                        navController.navigate("TrainingRecordDetail")
                    },
                    headlineContent = { Text(record) },
                    trailingContent = {
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                )
                Divider()
            }
        }
    }

    // ── 覆盖 75% 高的 Bottom-Sheet ──────────────
    if (showPlanSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPlanSheet = false },
            sheetState = planSheetState,
            dragHandle = null,
            modifier = Modifier.fillMaxHeight(0.75f)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("训练计划详情", style = MaterialTheme.typography.headlineSmall)
                    Text(currentPlan, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { showPlanSheet = false }) {
                        Text("关闭")
                    }
                }
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

    val circleBg = if (isToday || isSelected)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    else Color.Transparent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        if (showWeekDay) {
            Text(
                text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
            Spacer(Modifier.height(4.dp))
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(circleBg),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isTrained) Color(0xFF4CAF50) else Color.Transparent)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        isTrained -> Color.White
                        isToday   -> MaterialTheme.colorScheme.onPrimary
                        else      -> textColor
                    }
                )
            }
        }
    }
}

private fun generateWeekDates(center: LocalDate): List<LocalDate> {
    val firstDayOfWeek = center.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0..6).map { firstDayOfWeek.plusDays(it.toLong()) }
}

private fun generateMonthDates(center: LocalDate): List<LocalDate> {
    val firstOfMonth = center.withDayOfMonth(1)
    val lastOfMonth = center.withDayOfMonth(center.lengthOfMonth())
    val start = firstOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val end = lastOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val days = ChronoUnit.DAYS.between(start, end).toInt() + 1
    return (0 until days).map { start.plusDays(it.toLong()) }
}
