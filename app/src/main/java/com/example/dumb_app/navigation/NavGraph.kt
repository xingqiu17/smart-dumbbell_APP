// 文件路径：
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
import com.example.dumb_app.feature.profile.ProfileScreen
import com.example.dumb_app.feature.profile.EditBodyDataScreen
import com.example.dumb_app.feature.profile.EditUsernameScreen

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "workout"
    ) {
        composable("workout") {
            WorkoutScreen(navController)
        }
        composable("record") {
            RecordScreen(navController)
        }
        composable("profile") {
            ProfileScreen(navController)
        }
        composable("wifiConnect") {
            WifiConnectScreen(navController)
        }
        composable("editUsername") {
            EditUsernameScreen(navController)
        }
        // 新增编辑身体数据页面
        composable("editBodyData") {
            EditBodyDataScreen(navController)
        }
    }
}
