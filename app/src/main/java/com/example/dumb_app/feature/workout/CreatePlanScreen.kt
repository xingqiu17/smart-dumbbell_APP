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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlanScreen(nav: NavController) {
    // 准备 Repository
    val repo = remember { TrainingRepository(NetworkModule.apiService) }

    // 创建 ViewModel
    val vm: CreatePlanViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(clz: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CreatePlanViewModel(repo) as T
            }
        }
    )
    val ui by vm.uiState.collectAsState()

    // 首次加载已有计划
    LaunchedEffect(Unit) {
        vm.loadExistingPlan()
    }

    // 一旦保存成功，自动返回
    LaunchedEffect(ui.savedSessionId) {
        if (ui.savedSessionId != null) {
            nav.popBackStack()
        }
    }

    val ctx = LocalContext.current
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val picker = remember {
        DatePickerDialog(
            ctx,
            { _, y, m, d -> vm.onDateSelected(LocalDate.of(y, m + 1, d)) },
            ui.date.year, ui.date.monthValue - 1, ui.date.dayOfMonth
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 训练日期选择卡片
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

        // 列标题
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("顺序", Modifier.weight(1f))
            Text("动作", Modifier.weight(3f))
            Text("数量", Modifier.weight(2f))
            Spacer(Modifier.width(40.dp))
        }
        Divider(Modifier.padding(vertical = 4.dp))

        // 动态行列表
        LazyColumn {
            itemsIndexed(ui.rows) { idx, row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${idx + 1}", Modifier.weight(1f))

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
                        ExposedDropdownMenu(
                            expanded = exp,
                            onDismissRequest = { exp = false }
                        ) {
                            listOf("哑铃弯举", "卧推").forEach { act ->
                                DropdownMenuItem(
                                    text = { Text(act) },
                                    onClick = {
                                        vm.updateRow(idx, action = act)
                                        exp = false
                                    }
                                )
                            }
                        }
                    }

                    AndroidView(
                        factory = { c ->
                            EditText(c).apply {
                                inputType = InputType.TYPE_CLASS_NUMBER
                            }
                        },
                        update = { et ->
                            if (et.tag != true) {
                                et.tag = true
                                et.addTextChangedListener(object : TextWatcher {
                                    override fun afterTextChanged(s: Editable?) {
                                        vm.updateRow(
                                            idx,
                                            quantity = s?.toString()?.filter { it.isDigit() }
                                                .orEmpty()
                                        )
                                    }
                                    override fun beforeTextChanged(s: CharSequence?, s1: Int, s2: Int, s3: Int) {}
                                    override fun onTextChanged(s: CharSequence?, s1: Int, s2: Int, s3: Int) {}
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

                    IconButton(onClick = { vm.removeRow(idx) }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除行")
                    }
                }
                Divider()
            }
        }

        Spacer(Modifier.height(16.dp))

        // 添加新动作按钮
        Button(
            onClick = { vm.addRow() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("添加动作")
        }

        Spacer(Modifier.weight(1f))

        // 保存按钮
        val canSave = ui.rows.isNotEmpty() &&
                ui.rows.all { it.action.isNotBlank() && it.quantity.isNotBlank() }

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

        // 错误提示
        ui.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
