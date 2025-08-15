package com.example.dumb_app.feature.workout

import android.app.DatePickerDialog
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.EditText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dumb_app.core.network.NetworkModule
import com.example.dumb_app.core.repository.TrainingRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/* ================= 工具 ================= */

/** 仅保留数字的 TextWatcher */
private fun numWatcher(onChanged: (String) -> Unit) = object : TextWatcher {
    override fun afterTextChanged(s: Editable?) =
        onChanged(s?.toString()?.filter { it.isDigit() } ?: "")
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}

/** 显示用顺序号（只对 ActionRow 计数） */
private fun actionOrder(idx: Int, rows: List<PlanRow>): Int =
    rows.take(idx + 1).count { it is PlanRow.ActionRow }

/* ================================================================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlanScreen(nav: NavController) {
    /* -------- ViewModel -------- */
    val repo = remember { TrainingRepository(NetworkModule.apiService) }
    val vm: CreatePlanViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(clz: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CreatePlanViewModel(repo) as T
            }
        }
    )
    val ui by vm.uiState.collectAsState()

    /* -------- 生命周期副作用 -------- */
    LaunchedEffect(Unit) { vm.loadExistingPlan() }
    LaunchedEffect(ui.savedSessionId) {
        if (ui.savedSessionId != null) nav.popBackStack()
    }

    /* -------- 日期选择 -------- */
    val ctx = LocalContext.current
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val picker = remember {
        DatePickerDialog(
            ctx,
            { _, y, m, d -> vm.onDateSelected(LocalDate.of(y, m + 1, d)) },
            ui.date.year, ui.date.monthValue - 1, ui.date.dayOfMonth
        )
    }

    /* ================= 整体布局 ================= */
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        /* ---- 日期卡片 ---- */
        ElevatedCard(
            Modifier
                .fillMaxWidth()
                .clickable { picker.show() }
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("训练日期：", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(8.dp))
                Text(ui.date.format(fmt), style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(Modifier.height(16.dp))

        /* ---- 列标题 ---- */
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("顺序", Modifier.weight(1f))
            Text("动作 / 休息", Modifier.weight(3f))
            Text("数量 / 秒", Modifier.weight(2f))
            Spacer(Modifier.width(40.dp))
        }
        Divider(Modifier.padding(vertical = 4.dp))

        /* ================= 行列表 ================= */
        LazyColumn {
            itemsIndexed(ui.rows) { idx, row ->
                when (row) {
                    /* ───── 动作行 ───── */
                    is PlanRow.ActionRow -> {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            /* 顺序号 */
                            Text(
                                "${actionOrder(idx, ui.rows)}",
                                Modifier.weight(1f)
                            )

                            /* 动作下拉 */
                            var exp by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = exp,
                                onExpandedChange = { exp = !exp },
                                modifier = Modifier.weight(3f)
                            ) {
                                OutlinedTextField(
                                    value = row.action,
                                    onValueChange = {},
                                    readOnly = true,
                                    placeholder = { Text("请选择") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exp) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                                    listOf("哑铃弯举", "侧平举").forEach { act ->
                                        DropdownMenuItem(
                                            text = { Text(act) },
                                            onClick = {
                                                vm.updateActionRow(idx, action = act)
                                                exp = false
                                            }
                                        )
                                    }
                                }
                            }

                            /* 数量输入 */
                            AndroidView(
                                factory = { c ->
                                    EditText(c).apply { inputType = InputType.TYPE_CLASS_NUMBER }
                                },
                                update = { et ->
                                    if (et.tag != true) {
                                        et.tag = true
                                        et.addTextChangedListener(numWatcher { txt ->
                                            vm.updateActionRow(idx, quantity = txt)
                                        })
                                    }
                                    if (et.text.toString() != row.quantity) {
                                        et.setText(row.quantity)
                                        et.setSelection(row.quantity.length)
                                    }
                                },
                                modifier = Modifier
                                    .weight(2f)
                                    .height(56.dp)
                            )

                            /* 删除按钮 */
                            IconButton(onClick = { vm.removeRow(idx) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除动作")
                            }
                        }
                        Divider()
                    }

                    /* ───── 休息行 ───── */
                    is PlanRow.RestRow -> {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(Modifier.weight(1f))
                            Text("休息(秒)", Modifier.weight(3f))

                            AndroidView(
                                factory = { c ->
                                    EditText(c).apply { inputType = InputType.TYPE_CLASS_NUMBER }
                                },
                                update = { et ->
                                    if (et.tag != true) {
                                        et.tag = true
                                        et.addTextChangedListener(numWatcher { txt ->
                                            vm.updateRestRow(idx, txt)
                                        })
                                    }
                                    if (et.text.toString() != row.restSeconds) {
                                        et.setText(row.restSeconds)
                                        et.setSelection(row.restSeconds.length)
                                    }
                                },
                                modifier = Modifier
                                    .weight(2f)
                                    .height(56.dp)
                            )
                            Spacer(Modifier.width(40.dp))
                        }
                        Divider()
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        /* ---- 添加动作 ---- */
        Button(
            onClick = { vm.addRow() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("添加动作")
        }

        Spacer(Modifier.weight(1f))

        /* ---- 保存按钮 ---- */
        val actionRows = ui.rows.filterIsInstance<PlanRow.ActionRow>()
        val canSave = actionRows.isNotEmpty() &&
                actionRows.all { it.action.isNotBlank() && it.quantity.isNotBlank() }

        Button(
            onClick = { vm.savePlan() },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (ui.isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text("保存训练计划")
        }

        /* ---- 错误提示 ---- */
        ui.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
