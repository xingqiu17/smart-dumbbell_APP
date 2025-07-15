// app/src/main/java/com/example/dumb_app/DumbbellApp.kt

package com.example.dumb_app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.dumb_app.feature.profile.ProfileScreen
import com.example.dumb_app.feature.record.RecordScreen
import com.example.dumb_app.feature.workout.WorkoutScreen
import com.example.dumb_app.feature.workout.WifiConnectScreen
import com.example.dumb_app.ui.component.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DumbbellApp() {
    val navController = rememberNavController()
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "workout",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("workout")    { WorkoutScreen(navController) }
            composable("record")     { RecordScreen(navController) }
            composable("profile")    { ProfileScreen(navController)}
            composable("wifiConnect"){ WifiConnectScreen(navController) }
        }
    }
}
