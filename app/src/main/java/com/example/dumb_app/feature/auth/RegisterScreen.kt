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
import com.example.dumb_app.ui.theme.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController) {
    // RegisterViewModel 初始化
    val viewModel = remember { RegisterViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    var account by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var confirmPassword by remember { mutableStateOf(TextFieldValue("")) }
    var isRegisterEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(account, password, confirmPassword) {
        isRegisterEnabled = account.text.isNotEmpty() && password.text.isNotEmpty() && confirmPassword.text == password.text
    }

    // UI 状态变化：加载、成功、错误
    when (uiState) {
        is UiState.Loading -> {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        is UiState.Success -> {
            // 注册成功后跳转到编辑个人信息界面
            LaunchedEffect(Unit) {
                navController.navigate("editBodyData") {
                    popUpTo("register") { inclusive = true }
                }
            }
        }
        is UiState.Error -> {
            Text(text = "注册失败: ${(uiState as UiState.Error).msg}", color = MaterialTheme.colorScheme.error)
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("注册") }
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                    .padding(24.dp)
            ) {
                TextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text("账号") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("确认密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.register(account.text, password.text) },  // 调用 ViewModel 注册
                    enabled = isRegisterEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("注册")
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = { navController.navigate("login") }
                ) {
                    Text("已有账号？去登录")
                }
            }
        }
    }
}
