// app/src/main/java/com/example/dumb_app/navigation/NavGraph.kt

package com.example.dumb_app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dumb_app.feature.workout.WorkoutScreen
import com.example.dumb_app.feature.workout.WifiConnectScreen
import com.example.dumb_app.feature.record.RecordScreen
import com.example.dumb_app.feature.record.TrainingRecordDetailScreen
import com.example.dumb_app.feature.profile.ProfileScreen
import com.example.dumb_app.feature.profile.EditBodyDataScreen
import com.example.dumb_app.feature.profile.EditUsernameScreen
import com.example.dumb_app.feature.record.TrainingRecordDetailScreen

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "workout"
    ) {
        // 运动主界面
        composable("workout") {
            WorkoutScreen(navController)
        }
        // 记录主界面
        composable("record") {
            RecordScreen(navController)
        }
        // 个人主界面
        composable("profile") {
            ProfileScreen(navController)
        }
        // Wi-Fi 连接占位界面
        composable("wifiConnect") {
            WifiConnectScreen(navController)
        }

        // 编辑用户名
        composable("editUsername") {
            EditUsernameScreen(navController)
        }
        // 编辑身体数据
        composable("editBodyData") {
            EditBodyDataScreen(navController)
        }
        // 训练记录详情
        composable("TrainingRecordDetail") {
            TrainingRecordDetailScreen(navController)
        }

    }
}
