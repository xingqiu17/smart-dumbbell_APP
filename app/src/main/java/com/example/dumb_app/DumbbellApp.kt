// app/src/main/java/com/example/dumb_app/DumbbellApp.kt

package com.example.dumb_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dumb_app.navigation.NavGraph
import com.example.dumb_app.ui.component.BottomNavigationBar
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dumb_app.core.connectivity.wifi.WifiScanViewModel
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.example.dumb_app.core.model.Plan.PlanItemDto
import com.example.dumb_app.core.util.TrainingSession
import android.util.Log
import com.example.dumb_app.core.model.Plan.PlanSessionDto
import com.example.dumb_app.core.util.UserSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DumbbellApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "workout"

    val activity = LocalActivity.current as ComponentActivity           // ← 转成 ViewModelStoreOwner
    val wifiVm = viewModel<WifiScanViewModel>(viewModelStoreOwner = activity)


    val wifiEvent by wifiVm.wsEvents.collectAsState()
    LaunchedEffect(wifiEvent) {
        val event = wifiEvent
        if (event is WifiScanViewModel.WsEvent.TrainingStarted) {
            Log.d("DumbbellApp", "Global listener caught TrainingStarted event. Updating session and navigating...")

            // 1. 修正：根据最终的 DTO 定义，精确地创建对象
            val planItemsDto = event.items.mapIndexed { index, item ->
                PlanItemDto(
                    // ✅ 最终修正：构建一个完整的 PlanSessionDto
                    session = PlanSessionDto(
                        sessionId = event.sessionId.toInt(),
                        userId = UserSession.uid, // 从全局用户会话获取
                        date = event.date,        // 从设备事件中获取
                        complete = false          // 训练刚开始，设为 false
                    ),
                    type = item.type,
                    number = item.reps,
                    tOrder = index + 1,
                    tWeight = item.weight.toFloat(),
                    rest = item.rest
                )
            }

            // 2. 调用 update 方法来设置会话 (保持不变)
            TrainingSession.update(
                sessionId = event.sessionId.toInt(),
                items = planItemsDto,
                title = "来自设备的训练"
            )

            // 3. 导航到训练页 (保持不变)
            if (currentRoute != "training") {
                navController.navigate("training")
            }

            // 4. 消耗事件 (保持不变)
            wifiVm.clearEvent()
        }
    }
    val noBottomBarRoutes = listOf("login", "register")
    val showBottomBar = currentRoute !in noBottomBarRoutes

    Scaffold(
        bottomBar = {
            // 如果当前路由不是登录或注册，则显示底部导航栏
            if (showBottomBar) {
                BottomNavigationBar(selected = currentRoute) { destination ->
                    if (destination != currentRoute) {
                        navController.navigate(destination) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // ✅ 把共享的 wifiVm 传给 NavGraph
            NavGraph(navController = navController, wifiVm = wifiVm)
        }
    }
}
