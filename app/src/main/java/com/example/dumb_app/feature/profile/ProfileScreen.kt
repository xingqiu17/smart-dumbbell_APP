// app/src/main/java/com/example/dumb_app/feature/profile/ProfileScreen.kt
package com.example.dumb_app.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dumb_app.core.util.UserSession
import java.time.LocalDate
import java.time.Period

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val backEntry by navController.currentBackStackEntryAsState()
    val handle   = backEntry?.savedStateHandle

    val vm: ProfileViewModel = viewModel()
    val ui by vm.uiState.collectAsState()

    // 每次进入或从子页面返回，都触发一次加载
    LaunchedEffect(backEntry) {
        vm.loadProfile()
    }

    when (ui) {
        is ProfileUiState.Idle,
        is ProfileUiState.Loading -> {
            // Idle 和 Loading 一致处理
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ProfileUiState.Error -> {
            val msg = (ui as ProfileUiState.Error).msg
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("加载失败：$msg")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.loadProfile() }) {
                    Text("重试")
                }
            }
        }
        is ProfileUiState.Success -> {
            // 拿到最新用户信息
            val user = (ui as ProfileUiState.Success).user

            // 回填监听：若从编辑界面带了字段回来，则再次整体刷新
            handle?.getLiveData<String>("username")
                ?.observe(backEntry!!) {
                    vm.loadProfile().also { handle.remove<String>("username") }
                }
            handle?.getLiveData<String>("birthDate")
                ?.observe(backEntry!!) {
                    vm.loadProfile().also { handle.remove<String>("birthDate") }
                }
            handle?.getLiveData<Int>("height")
                ?.observe(backEntry!!) {
                    vm.loadProfile().also { handle.remove<Int>("height") }
                }
            handle?.getLiveData<Int>("weight")
                ?.observe(backEntry!!) {
                    vm.loadProfile().also { handle.remove<Int>("weight") }
                }
            handle?.getLiveData<String>("gender")
                ?.observe(backEntry!!) {
                    vm.loadProfile().also { handle.remove<String>("gender") }
                }
            handle?.getLiveData<Int>("aim")
                ?.observe(backEntry!!) {
                    vm.loadProfile().also { handle.remove<Int>("aim") }
                }
            handle?.getLiveData<Float>("trainWeight")
                ?.observe(backEntry!!) {
                    vm.loadProfile().also { handle.remove<Float>("trainWeight") }
                }

            // 解析字段
            val username    = user.name
            val birthDate   = LocalDate.parse(user.birthday)
            val genderStr   = when (user.gender) {
                1 -> "男"
                2 -> "女"
                else -> "不便透露"
            }
            val heightCm    = user.height.toInt()
            val weightKg    = user.weight.toInt()
            val age         = Period.between(birthDate, LocalDate.now()).years
            val goalText    = aimIntToStr(user.aim)
            val trainWeight = user.hwWeight

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("个人信息", style = MaterialTheme.typography.headlineMedium)

                    // 头像 & 用户名
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("editUsername") },
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(username, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "点击修改用户名",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }

                    // 身体数据
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("editBodyData") },
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("身体数据", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("年龄：${age}岁", style = MaterialTheme.typography.bodyMedium)
                                Text("性别：$genderStr", style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("身高：${heightCm}cm", style = MaterialTheme.typography.bodyMedium)
                                Text("体重：${weightKg}kg", style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                "点击查看/修改",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 训练数据
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("editTrainData") },
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("训练目标：$goalText", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "训练配重：${"%.1f".format(trainWeight)}kg",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** aim 枚举 → 中文描述 */
private fun aimIntToStr(code: Int): String = when (code) {
    1    -> "手臂"
    2    -> "肩部"
    3    -> "胸部"
    4    -> "背部"
    5    -> "腿部"
    else -> "无目标"
}
