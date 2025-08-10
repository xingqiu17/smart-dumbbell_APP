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
import androidx.navigation.NavType
import androidx.navigation.navArgument

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
        composable(
            // 1. Define the route with a placeholder for the "time" argument
            route = "rest/{time}",
            // 2. Specify that the "time" argument is an Integer
            arguments = listOf(navArgument("time") { type = NavType.IntType })
        ) { backStackEntry ->
            // 3. Retrieve the integer value from the navigation arguments.
            //    Provide a safe default (e.g., 60 seconds) if it's somehow not passed.
            val time = backStackEntry.arguments?.getInt("time") ?: 5

            // 4. Pass the retrieved time to the RestScreen's `totalSeconds` parameter.
            RestScreen(
                navController = navController,
                wifiVm = wifiVm,
                totalSeconds = time
            )
        }
        composable("training") {
            TrainingScreen(
                navController = navController,
                wifiVm = wifiVm
            )
        }

    }
}
