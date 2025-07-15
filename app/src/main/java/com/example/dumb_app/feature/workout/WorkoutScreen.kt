// app/src/main/java/com/example/dumb_app/feature/workout/WorkoutScreen.kt

package com.example.dumb_app.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dumb_app.ui.component.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(nav: NavController) {
    // ── Bottom-Sheet 状态 ───────────────────────────
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }

    // ── Scaffold：顶部栏 + 底部导航 ───────────────────
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { nav.navigate("wifiConnect") }) {
                        Icon(Icons.Default.Add, contentDescription = "添加设备")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(bottom = 64.dp),
            contentAlignment = Alignment.Center
        ) {
            // ── 圆形大按钮 ────────────────────────
            Surface(
                modifier = Modifier
                    .size(220.dp)
                    .shadow(8.dp, CircleShape),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                onClick = { showSheet = true }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .rotate(135f),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "开始运动",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }

    // ── 覆盖 75% 高的 Bottom-Sheet ──────────────
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            dragHandle = null,
            modifier = Modifier.fillMaxHeight(0.75f)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("训练模式 / 计数 / 选择动作 ……", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
