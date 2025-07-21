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
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dumb_app.core.util.UserSession
import java.time.LocalDate
import java.time.Period

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {

    /* ---------- Nav & Saved-State ---------- */
    val backEntry by navController.currentBackStackEntryAsState()
    val handle    = backEntry?.savedStateHandle

    /* ---------- 初始值：来自 UserSession ---------- */
    val user      = UserSession.currentUser
    var username  by remember { mutableStateOf(user?.name ?: "张三") }
    var birthDate by remember {
        mutableStateOf(
            user?.birthday?.let { LocalDate.parse(it) }    // UserDto.birthday 是字符串
                ?: LocalDate.of(1990, 1, 1)
        )
    }
    var gender by remember {
        mutableStateOf(
            when (user?.gender) {
                0    -> "不便透露"
                1    -> "男"
                2    -> "女"
                else -> "不便透露"
            }
        )
    }
    var height    by remember { mutableStateOf(user?.height?.toInt() ?: 175) }    // cm
    var weight    by remember { mutableStateOf(user?.weight?.toInt() ?: 65) }     // kg

    /* 训练数据 */
    var goal        by remember { mutableStateOf(aimIntToStr(user?.aim ?: 0)) }
    var trainWeight by remember { mutableStateOf(user?.hwWeight ?: 0f) }

    /* ---------- 监听回传 ---------- */
    // 用户名
    handle?.getLiveData<String>("username")
        ?.observe(backEntry!!) {
            username = it
            handle.remove<String>("username")
        }

    // 出生日期 / 身高 / 体重 / 性别
    handle?.getLiveData<String>("birthDate")
        ?.observe(backEntry!!) {
            birthDate = LocalDate.parse(it)
            handle.remove<String>("birthDate")
        }

    handle?.getLiveData<Int>("height")
        ?.observe(backEntry!!) {
            height = it
            handle.remove<Int>("height")
        }
    handle?.getLiveData<Int>("weight")
        ?.observe(backEntry!!) {
            weight = it
            handle.remove<Int>("weight")
        }
    handle?.getLiveData<String>("gender")
        ?.observe(backEntry!!) {
            gender = it
            handle.remove<String>("gender")
        }

    // 训练数据
    handle?.getLiveData<Int>("aim")
        ?.observe(backEntry!!) {
            goal = aimIntToStr(it)
            handle.remove<Int>("aim")
        }
    handle?.getLiveData<Float>("trainWeight")
        ?.observe(backEntry!!) {
            trainWeight = it
            handle.remove<Float>("trainWeight")
        }

    /* ---------- UI ---------- */
    val age = Period.between(birthDate, LocalDate.now()).years   // ← 去掉错误的强转

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

            /* --- 头像 & 用户名 --- */
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
                        null,
                        Modifier
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
                    Icon(Icons.Default.ChevronRight, null)
                }
            }

            /* --- 身体数据 --- */
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

            /* --- 训练数据 --- */
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
                    Text("训练目标：$goal", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "训练配重：${"%.1f".format(trainWeight)} kg",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/* aim 枚举 → 中文描述 */
private fun aimIntToStr(code: Int): String = when (code) {
    1    -> "手臂"
    2    -> "肩部"
    3    -> "胸部"
    4    -> "背部"
    5    -> "腿部"
    else -> "无目标"
}
