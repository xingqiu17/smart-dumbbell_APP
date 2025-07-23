// app/src/main/java/com/example/dumb_app/feature/profile/EditTrainDataScreen.kt
package com.example.dumb_app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dumb_app.core.util.UserSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTrainDataScreen(navController: NavController) {

    /* ---------- 读取当前用户的已有训练数据 ---------- */
    val user = UserSession.currentUser
    val initAimStr   = aimIntToStr(user?.aim ?: 0)
    val initWeight   = user?.hwWeight?.takeIf { it > 0f }?.toString().orEmpty()

    /* ---------- 本地状态 ---------- */
    var targetExpanded by remember { mutableStateOf(false) }
    var target         by remember { mutableStateOf(initAimStr) }    // ← 用已有值初始化
    var weightInput    by remember { mutableStateOf(initWeight) }    // ← 用已有值初始化

    val viewModel: EditTrainDataViewModel = remember { EditTrainDataViewModel() }
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
            Modifier
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
                    value = target,
                    readOnly = true,
                    onValueChange = {},
                    label = { Text("训练目标") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded)
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = targetExpanded,
                    onDismissRequest = { targetExpanded = false }
                ) {
                    listOf("无目标", "手臂", "肩部", "胸部", "背部", "腿部").forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = { target = it; targetExpanded = false }
                        )
                    }
                }
            }

            /* ───────── 训练配重 ───────── */
            OutlinedTextField(
                value = weightInput,
                onValueChange = { new ->
                    if (new.matches(Regex("""\d*\.?\d*"""))) weightInput = new
                },
                label = { Text("训练配重 (kg)") },
                placeholder = { Text("例如 12.5") },
                singleLine = true,
                isError = !isValidWeight,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))

            /* ───────── 保存按钮 ───────── */
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(uiState) {
                if (uiState is TrainUiState.Success) {
                    val aimCode = targetToCode(target)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.apply {
                            set("aim",         aimCode)
                            set("trainWeight", weightInput.toFloatOrNull() ?: 0f)
                        }
                    navController.popBackStack()
                }
            }

            Button(
                enabled = isValidWeight && uiState !is TrainUiState.Loading,
                onClick = {
                    val aimCode = targetToCode(target)
                    val weight  = weightInput.toFloatOrNull() ?: 0f
                    viewModel.submit(aimCode, weight)
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                if (uiState is TrainUiState.Loading) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp).padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text("保存")
            }
        }
    }
}

/* 把中文目标映射为后端枚举 Int */
private fun targetToCode(t: String): Int = when (t) {
    "手臂" -> 1; "肩部" -> 2; "胸部" -> 3; "背部" -> 4; "腿部" -> 5
    else   -> 0
}

/* aim Int → 中文，用于初始化 */
private fun aimIntToStr(code: Int): String = when (code) {
    1 -> "手臂"; 2 -> "肩部"; 3 -> "胸部"; 4 -> "背部"; 5 -> "腿部"
    else -> "无目标"
}
