// app/src/main/java/com/example/dumb_app/feature/profile/EditUsernameScreen.kt
package com.example.dumb_app.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel          // ← 别忘了导入
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUsernameScreen(
    navController: NavController,
    /** 这里改成 vm，避免与 viewModel() 函数重名 */
    vm: EditUsernameViewModel = viewModel()
) {
    /* SavedStateHandle 用来回写新名字，ProfileScreen 正在监听 */
    val backEntry by navController.currentBackStackEntryAsState()
    val handle: SavedStateHandle? = backEntry?.savedStateHandle

    var newUsername by remember { mutableStateOf("") }
    val uiState by vm.uiState.collectAsState()

    /** 保存成功 → 退出页面 */
    LaunchedEffect(uiState) {
        if (uiState is NameUiState.Success) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("修改用户名") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = newUsername,
                onValueChange = { newUsername = it },
                label = { Text("用户名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))

            Button(
                enabled = newUsername.isNotBlank() && uiState !is NameUiState.Loading,
                onClick = {
                    handle?.let { vm.submit(newUsername.trim(), it) }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                if (uiState is NameUiState.Loading) {
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
