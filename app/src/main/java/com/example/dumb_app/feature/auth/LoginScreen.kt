package com.example.dumb_app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.navigation.NavController
import com.example.dumb_app.ui.component.BottomNavigationBar

// ✓ 最终版  —— app/feature/auth/LoginScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {

    // ① ViewModel
    val vm = remember { LoginViewModel() }
    val uiState by vm.uiState.collectAsState()

    // ② 输入框状态
    var account by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    val canLogin = account.text.length == 11 && password.text.length in 6..16

    // ③ 登录成功 → 跳首页
    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            navController.navigate("workout") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = { SmallTopAppBar(title = { Text("登录") }) },
        contentWindowInsets = WindowInsets(0)
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            Alignment.Center
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(24.dp)
            ) {
                TextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text("手机号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { vm.login(account.text, password.text) },
                    enabled = canLogin,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("登录") }

                Spacer(Modifier.height(8.dp))

                TextButton(onClick = { navController.navigate("register") }) {
                    Text("没有账号？去注册")
                }

                // ④ 状态提示
                when (uiState) {
                    is UiState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                    is UiState.Error   -> Text(
                        (uiState as UiState.Error).msg,
                        color = MaterialTheme.colorScheme.error
                    )
                    else -> {}
                }
            }
        }
    }
}
