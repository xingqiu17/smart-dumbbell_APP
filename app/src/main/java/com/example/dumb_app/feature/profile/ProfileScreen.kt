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
import androidx.lifecycle.get
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import java.time.LocalDate
import java.time.Period

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    // 监听当前 NavBackStackEntry，以取回 savedStateHandle
    val backStackEntry by navController.currentBackStackEntryAsState()
    val handle = backStackEntry?.savedStateHandle

    // 本地状态
    var username by remember { mutableStateOf("张三") }
    var birthDate by remember { mutableStateOf(LocalDate.of(1990, 1, 1)) }
    var gender by remember { mutableStateOf("男") }
    var height by remember { mutableStateOf(175) }  // cm
    var weight by remember { mutableStateOf(65) }   // kg
    var goal by remember { mutableStateOf("增肌") }

    // 从 editUsername 回传
    handle
        ?.getLiveData<String>("username")
        ?.observe(backStackEntry!!) { newUsername ->
            username = newUsername
            handle.remove<String>("username")
        }

    // 从 editBodyData 回传出生日期、身高、体重、性别
    handle
        ?.getLiveData<String>("birthDate")
        ?.observe(backStackEntry!!) { bdString ->
            bdString?.let { birthDate = LocalDate.parse(it) }
            handle.remove<String>("birthDate")
        }
    handle
        ?.getLiveData<Int>("height")
        ?.observe(backStackEntry!!) { newHeight ->
            height = newHeight
            handle.remove<Int>("height")
        }
    handle
        ?.getLiveData<Int>("weight")
        ?.observe(backStackEntry!!) { newWeight ->
            weight = newWeight
            handle.remove<Int>("weight")
        }
    handle
        ?.getLiveData<String>("gender")
        ?.observe(backStackEntry!!) { newGender ->
            gender = newGender
            handle.remove<String>("gender")
        }

    val age = Period.between(birthDate, LocalDate.now()).years
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background     // ← 用主题背景色
    ) {

        Column(
            modifier = Modifier
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
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
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
                    Column(modifier = Modifier.weight(1f)) {
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
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("身体数据", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("年龄：${age}岁", style = MaterialTheme.typography.bodyMedium)
                        Text("性别：$gender", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("身高：${height}cm", style = MaterialTheme.typography.bodyMedium)
                        Text("体重：${weight}kg", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "点击查看/修改",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 训练目标
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("训练目标：", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(8.dp))
                    Text(goal, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
