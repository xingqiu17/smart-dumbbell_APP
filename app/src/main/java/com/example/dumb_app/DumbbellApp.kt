// app/src/main/java/com/example/dumb_app/DumbbellApp.kt

package com.example.dumb_app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.example.dumb_app.feature.workout.WorkoutScreen
import com.example.dumb_app.feature.workout.WifiConnectScreen
import com.example.dumb_app.ui.component.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DumbbellApp() {
    val navController = rememberNavController()
    // 监听当前 route
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "workout"

    Scaffold(
        bottomBar = {
            BottomNavigationBar(selected = currentRoute) { dest ->
                if (dest != currentRoute) {
                    navController.navigate(dest) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "workout",
            modifier = Modifier.padding(padding)
        ) {
            composable("workout")   { WorkoutScreen(navController) }
            composable("record")    { /* TODO: RecordScreen(navController) */ }
            composable("profile")   { /* TODO: ProfileScreen(navController) */ }
            composable("wifiConnect"){ WifiConnectScreen(navController) }
        }
    }
}
