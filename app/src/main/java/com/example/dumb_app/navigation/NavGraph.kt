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
            /* TODO: 以后填 RecordScreen(navController) */
        }
        composable("profile") {
            /* TODO: 以后填 ProfileScreen(navController) */
        }
        composable("wifiConnect") {
            WifiConnectScreen(navController)
        }
    }
}
