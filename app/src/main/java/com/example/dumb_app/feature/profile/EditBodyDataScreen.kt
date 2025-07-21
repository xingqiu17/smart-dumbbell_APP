package com.example.dumb_app.feature.profile

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dumb_app.core.util.UserSession
import java.time.LocalDate
import java.time.Period

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBodyDataScreen(
    navController: NavController,
    viewModel: EditBodyDataViewModel = viewModel()
) {
    /* ---------- Nav & SavedState ---------- */
    val backEntry by navController.currentBackStackEntryAsState()
    val handle: SavedStateHandle? = backEntry?.savedStateHandle

    /* ---------- 会话初始化 ---------- */
    val user = UserSession.currentUser ?: return

    /** 出生日期（字符串 → LocalDate） */
    var birthDate by remember { mutableStateOf(LocalDate.parse(user.birthday)) }

    /** 身高 / 体重 */
    var heightInput by remember { mutableStateOf(user.height.toInt().toString()) }
    var weightInput by remember { mutableStateOf(user.weight.toInt().toString()) }

    /** 性别：0=不便透露 1=男 2=女 */
    val genderOptions = listOf("不便透露", "男", "女")
    var genderExpanded by remember { mutableStateOf(false) }
    var gender by remember {
        mutableStateOf(
            when (user.gender) {
                1    -> "男"
                2    -> "女"
                else -> "不便透露"
            }
        )
    }

    /* ---------- 输入合法性 ---------- */
    val isValidHeight = heightInput.toFloatOrNull() != null
    val isValidWeight = weightInput.toFloatOrNull() != null

    /* ---------- DatePicker ---------- */
    val context = LocalContext.current
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, y, m0, d ->
                birthDate = LocalDate.of(y, m0 + 1, d)   // m0 0-based
            },
            birthDate.year,
            birthDate.monthValue - 1,
            birthDate.dayOfMonth
        )
    }

    /* ---------- VM 状态监听 ---------- */
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState) {
        if (uiState is BodyUiState.Success) navController.popBackStack()
    }

    /* ---------- UI ---------- */
    val age = Period.between(birthDate, LocalDate.now()).years

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("修改身体数据") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            /* ---------- 出生日期（仅展示的可点击卡片） ---------- */
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        datePickerDialog.datePicker.updateDate(
                            birthDate.year,
                            birthDate.monthValue - 1,
                            birthDate.dayOfMonth
                        )
                        datePickerDialog.show()
                    }
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("出生日期：${birthDate}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    Text("年龄 $age 岁", style = MaterialTheme.typography.bodyMedium)
                }
            }

            /* ---------- 身高 ---------- */
            OutlinedTextField(
                value = heightInput,
                onValueChange = { heightInput = it },
                label = { Text("身高 (cm)") },
                isError = !isValidHeight,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            /* ---------- 体重 ---------- */
            OutlinedTextField(
                value = weightInput,
                onValueChange = { weightInput = it },
                label = { Text("体重 (kg)") },
                isError = !isValidWeight,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            /* ---------- 性别下拉 ---------- */
            ExposedDropdownMenuBox(
                expanded = genderExpanded,
                onExpandedChange = { genderExpanded = !genderExpanded }
            ) {
                OutlinedTextField(
                    value = gender,
                    onValueChange = {},
                    readOnly = true,
                    label  = { Text("性别") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(genderExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false }
                ) {
                    genderOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                gender = option
                                genderExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            /* ---------- 保存 ---------- */
            Button(
                enabled = isValidHeight && isValidWeight && uiState !is BodyUiState.Loading,
                onClick = {
                    val genderCode = genderOptions.indexOf(gender)   // 0,1,2
                    handle?.let {
                        viewModel.submit(
                            birthdayStr = birthDate.toString(),
                            heightCm    = heightInput.toFloat(),
                            weightKg    = weightInput.toFloat(),
                            genderCode  = genderCode,
                            handle      = it
                        )
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                if (uiState is BodyUiState.Loading) {
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
