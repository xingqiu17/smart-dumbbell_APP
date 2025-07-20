package com.example.dumb_app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController) {
    /* ---------- ViewModel ---------- */
    val viewModel = remember { RegisterViewModel() }
    val uiState  by viewModel.uiState.collectAsState()

    /* ---------- 本地状态 ---------- */
    var account         by remember { mutableStateOf(TextFieldValue("")) }
    var password        by remember { mutableStateOf(TextFieldValue("")) }
    var confirmPassword by remember { mutableStateOf(TextFieldValue("")) }
    val isRegisterEnabled = account.text.isNotEmpty() &&
            password.text.isNotEmpty() &&
            password.text == confirmPassword.text

    /* ---------- 状态监听 ---------- */
    when (uiState) {
        is UiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Success -> {
            LaunchedEffect(Unit) {
                navController.navigate("editBodyData") {
                    popUpTo("register") { inclusive = true }
                }
            }
        }
        is UiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "注册失败: ${(uiState as UiState.Error).msg}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        UiState.Idle -> { /* 初始什么都不画 */ }
    }

    /* ---------- UI ---------- */
    Scaffold(
        topBar = { TopAppBar(title = { Text("注册") }) },
        contentWindowInsets = WindowInsets(0)
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color  = MaterialTheme.colorScheme.surface,
                        shape  = MaterialTheme.shapes.medium
                    )
                    .padding(24.dp)
            ) {
                TextField(
                    value       = account,
                    onValueChange = { account = it },
                    label         = { Text("账号") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                TextField(
                    value       = password,
                    onValueChange = { password = it },
                    label         = { Text("密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                TextField(
                    value       = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label         = { Text("确认密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick  = { viewModel.register(account.text, password.text) },
                    enabled  = isRegisterEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("注册") }

                Spacer(Modifier.height(16.dp))

                TextButton(onClick = { navController.navigate("login") }) {
                    Text("已有账号？去登录")
                }
            }
        }
    }
}
