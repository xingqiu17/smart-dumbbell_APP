package com.example.dumb_app.feature.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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

    /* 监听日期变化 → 拉取计划 */
    LaunchedEffect(selectedDate) {
        vm.loadPlans(selectedDate.toString())      // yyyy-MM-dd
    }

    /* 订阅状态 */
    val uiState by vm.uiState.collectAsState()

    /* —— Bottom-Sheet —— */
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    var currentPlan by remember { mutableStateOf("") }

    /* ====== 训练计划列表（字符串展示） ====== */
    val trainingPlans: List<String> = when (uiState) {
        is PlanUiState.Success -> (uiState as PlanUiState.Success).items.map { item ->
            "类型${item.type} × ${item.number}"
        }
        else -> emptyList()
    }

    /* ====== 训练记录日期高亮（TODO: 接真实接口） ====== */
    val trainingRecordsDates = remember { listOf(today.minusDays(2), today) }

    /* ====== 顶部 Loading / Error / Empty ====== */
    when (uiState) {
        PlanUiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        is PlanUiState.Error -> Text(
            text = (uiState as PlanUiState.Error).msg,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center
        )
        PlanUiState.Empty -> Text(
            text = "今日暂无训练计划",
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center
        )
        else -> {}   // Success 时不额外提示
    }

    /* 主体 UI（原布局基本未动） */
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
            /* —— 日历头 —— */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${selectedDate.year} / ${selectedDate.monthValue}",
                    style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { isWeekView = !isWeekView }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "切换视图")
                }
            }

            /* —— 周 / 月 视图 —— */
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

            /* —— 当日训练计划 —— */
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
                            showSheet = true
                        },
                        headlineContent = { Text(plan) },
                        trailingContent = {
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    )
                    Divider()
                }
            }

            /* —— 训练记录（假数据，占位） —— */
            Spacer(Modifier.height(16.dp))
            Text(
                text = "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日 训练记录",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )
            Column {
                listOf("第一次训练", "第二次训练").forEach { record ->
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

        /* —— Plan Detail Bottom-Sheet —— */
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
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
                        Button(onClick = { showSheet = false }) { Text("关闭") }
                    }
                }
            }
        }
    }
}

/* ========== 组件 & 工具函数（原逻辑保持） ========== */

@Composable
private fun MonthGrid(
    center: LocalDate,
    today: LocalDate,
    selectedDate: LocalDate,
    trainingDates: List<LocalDate>,
    onSelect: (LocalDate) -> Unit
) {
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
                            showWeekDay = false,
                            onClick = { onSelect(date) }
                        )
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
    val first = center.withDayOfMonth(1)
    val last  = center.withDayOfMonth(center.lengthOfMonth())
    val start = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val end   = last.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val days  = ChronoUnit.DAYS.between(start, end).toInt() + 1
    return (0 until days).map { start.plusDays(it.toLong()) }
}
