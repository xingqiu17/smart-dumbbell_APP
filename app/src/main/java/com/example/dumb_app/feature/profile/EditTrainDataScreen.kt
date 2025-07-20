// app/src/main/java/com/example/dumb_app/feature/profile/EditTrainDataScreen.kt
package com.example.dumb_app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTrainDataScreen(navController: NavController) {

    // ↓ 本地状态
    var targetExpanded by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf("无目标") }
    val viewModel: EditTrainDataViewModel = remember { EditTrainDataViewModel() }

    var weightInput by remember { mutableStateOf("") }   // 文本框原始值
    val isValidWeight = weightInput.matches(Regex("""\d*\.?\d*"""))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("训练数据") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            /* ───────── 训练目标 ───────── */
            ExposedDropdownMenuBox(
                expanded = targetExpanded,
                onExpandedChange = { targetExpanded = !targetExpanded }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = target,
                    onValueChange = {},
                    label = { Text("训练目标") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(targetExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = targetExpanded,
                    onDismissRequest = { targetExpanded = false }
                ) {
                    listOf("无目标", "手臂", "肩部", "胸部", "背部", "腿部").forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = {
                                target = it
                                targetExpanded = false
                            }
                        )
                    }
                }
            }

            /* ───────── 训练配重 ───────── */
            OutlinedTextField(
                value = weightInput,
                onValueChange = { new ->
                    // 只接受数字或单个小数点
                    if (new.matches(Regex("""\d*\.?\d*"""))) {
                        weightInput = new
                    }
                },
                label = { Text("训练配重 (kg)") },
                placeholder = { Text("例如 12.5") },
                isError = !isValidWeight,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))

            /* ───────── 保存按钮 ───────── */
            val uiState by viewModel.uiState.collectAsState()

            // ① 监听保存成功 ➜ 退出页面
            LaunchedEffect(uiState) {
                if (uiState is TrainUiState.Success) {
                    navController.popBackStack()
                }
            }

            Button(
                enabled = isValidWeight && uiState !is TrainUiState.Loading,
                onClick = {
                    val aimCode = when (target) {
                        "手臂" -> 1; "肩部" -> 2; "胸部" -> 3; "背部" -> 4; "腿部" -> 5
                        else  -> 0
                    }
                    val weight = weightInput.toFloatOrNull() ?: 0f
                    viewModel.submit(aimCode, weight)   // ← 现在不传 uid
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                if (uiState is TrainUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text("保存")
            }

        }
    }
}
