// app/src/main/java/com/example/dumb_app/navigation/NavGraph.kt

package com.example.dumb_app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dumb_app.core.connectivity.wifi.WifiScanViewModel
import com.example.dumb_app.feature.workout.WorkoutScreen
import com.example.dumb_app.feature.workout.WifiConnectScreen
import com.example.dumb_app.feature.record.RecordScreen
import com.example.dumb_app.feature.record.TrainingRecordDetailScreen
import com.example.dumb_app.feature.profile.ProfileScreen
import com.example.dumb_app.feature.profile.EditBodyDataScreen
import com.example.dumb_app.feature.profile.EditUsernameScreen
import com.example.dumb_app.feature.profile.EditTrainDataScreen
import com.example.dumb_app.feature.auth.LoginScreen
import com.example.dumb_app.feature.auth.RegisterScreen
import com.example.dumb_app.feature.workout.CreatePlanScreen
import com.example.dumb_app.feature.workout.RestScreen
import com.example.dumb_app.feature.workout.TrainingScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    wifiVm: WifiScanViewModel
) {

    val firstRoute = if (/* 已登录？ */ false) "workout" else "login"

    NavHost(
        navController = navController,
        startDestination = firstRoute
    ) {
        // 运动主界面
        composable("workout") {
            WorkoutScreen(
                nav = navController,
                wifiVm = wifiVm               // ← 传给 WorkoutScreen
            )
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

        composable("login")    { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("EditTrainData") { EditTrainDataScreen(navController) }
        composable("createPlan") { CreatePlanScreen(navController) }
        composable("rest") { RestScreen(navController) }
        composable("training") {
            TrainingScreen(
                navController = navController,
                wifiVm = wifiVm
            )
        }

    }
}
