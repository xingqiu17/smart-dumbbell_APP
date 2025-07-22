package com.example.dumb_app.feature.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dumb_app.core.model.Plan.PlanDayDto
import com.example.dumb_app.core.model.Plan.PlanItemDto
import com.example.dumb_app.core.model.Log.LogDayDto          // ← 新增
import com.example.dumb_app.core.model.Log.LogItemDto         // ← 新增
import com.example.dumb_app.feature.record.LogUiState         // ← 新增
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    navController: NavController,
    vm: RecordViewModel = viewModel()
) {
    var isWeekView by remember { mutableStateOf(true) }
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }


    // 监听日期变化 → 拉取当天所有会话和训练记录
    LaunchedEffect(isWeekView, selectedDate) {
        vm.loadPlans(selectedDate.toString())
        vm.loadLogs(selectedDate.toString())    // ← 新增
        val datesToLoad = if (isWeekView) {
            generateWeekDates(selectedDate)
        } else {
            generateMonthDates(selectedDate)
        }
        // 2. 调用 ViewModel 加载这些日期的记录，VM 会把有记录的日期写入 trainingDates StateFlow
        vm.loadTrainingDates(datesToLoad)
    }
    // Plan 部分状态
    val planUi by vm.planState.collectAsState()
    // Log 部分状态
    val logUi  by vm.logState.collectAsState()   // ← 新增

    // BottomSheet 状态（仅给 Plan 用）
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    var currentItems by remember { mutableStateOf<List<PlanItemDto>>(emptyList()) }

    // 从 planUi 中取出 sessions 列表（按 sessionId 升序）
    val sessions: List<PlanDayDto> = when (planUi) {
        is PlanUiState.Success -> (planUi as PlanUiState.Success).sessions
            .sortedBy { it.session.sessionId }
        else -> emptyList()
    }

    // 日历中高亮的训练记录日（示例，后续接真实接口，可改用 logUi）
    // —— 改动1：用真实 logUi 数据来高亮 ——
    val trainingRecordsDates by vm.trainingDates.collectAsState()

    // Plan 顶部 Loading / Error / Empty 提示（不动）
    when (planUi) {
        PlanUiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        is PlanUiState.Error -> Text(
            text = (planUi as PlanUiState.Error).msg,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center
        )
        PlanUiState.Empty -> Text(
            "今日暂无训练计划",
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center
        )
        else -> { /* Success 不额外提示 */ }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // —— 日历头部 ——
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

            // —— 周视图 or 月视图 ——
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
                            isTrained = trainingRecordsDates.contains(date)
                        ) { selectedDate = date }
                    }
                }
            } else {
                MonthGrid(
                    center = selectedDate,
                    today = today,
                    selectedDate = selectedDate,
                    trainingDates = trainingRecordsDates,
                    onSelect = { selectedDate = it }
                )
            }

            Spacer(Modifier.height(16.dp))
            Divider()

            // —— 当日训练计划 列表 ——
            Text(
                text = "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日 训练计划",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )

            // —— 改动2：如果 sessions 为空，显示占位文本 ——
            if (sessions.isEmpty()) {
                Text(
                    "当日暂无训练计划",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                Column {
                    sessions.forEachIndexed { idx, day ->
                        val label = "计划 ${idx + 1}"
                        ListItem(
                            modifier = Modifier.clickable {
                                currentItems = day.items
                                showSheet = true
                            },
                            headlineContent = { Text(label) },
                            supportingContent = {
                                Text("共 ${day.items.size} 组动作")
                            },
                            trailingContent = {
                                Icon(Icons.Default.ArrowForward, contentDescription = null)
                            }
                        )
                        Divider()
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // —— 训练记录 列表 —— （从这里开始改动）
            Text(
                text = "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日 训练记录",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )

            when (logUi) {
                LogUiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is LogUiState.Error -> Text(
                    text = (logUi as LogUiState.Error).msg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    textAlign = TextAlign.Center
                )
                LogUiState.Empty -> Text(
                    "当日暂无训练记录",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    textAlign = TextAlign.Center
                )
                is LogUiState.Success -> {
                    val day = (logUi as LogUiState.Success).record
                    // 如果只有一条，就直接展示
                    ListItem(
                        modifier = Modifier.clickable {
                            vm.selectLog(day)
                            navController.navigate("TrainingRecordDetail")
                        },
                        headlineContent = { Text("记录 1") },
                        supportingContent = { Text("共 ${day.items.size} 组动作") },
                        trailingContent  = { Icon(Icons.Default.ArrowForward, null) }
                    )
                }
            }
        }

        // —— 底部弹窗：展示当前会话的动作明细 ——
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                dragHandle = null,
                modifier = Modifier.fillMaxHeight(0.75f)
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                ) {
                    Text("训练计划详情", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(currentItems) { idx, item ->
                            Text(
                                text = "${idx + 1}. 类型${item.type} × ${item.number}次，配重${item.tWeight}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showSheet = false }, Modifier.align(Alignment.End)) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}

/**
 * 月视图网格
 */
@Composable
private fun MonthGrid(
    center: LocalDate,
    today: LocalDate,
    selectedDate: LocalDate,
    trainingDates: List<LocalDate>,
    onSelect: (LocalDate) -> Unit
) {
    // 星期标签
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
    val monthDates = generateMonthDates(center)
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
                        ) { onSelect(date) }
                    }
                }
            }
        }
    }
}

/**
 * 单个日期格子
 */
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
            .clickable(onClick = onClick)
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
    val first = center.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0..6).map { first.plusDays(it.toLong()) }
}

private fun generateMonthDates(center: LocalDate): List<LocalDate> {
    val firstOfMonth = center.withDayOfMonth(1)
    val lastOfMonth  = center.withDayOfMonth(center.lengthOfMonth())
    val start = firstOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val end   = lastOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val days  = ChronoUnit.DAYS.between(start, end).toInt() + 1
    return (0 until days).map { start.plusDays(it.toLong()) }
}
