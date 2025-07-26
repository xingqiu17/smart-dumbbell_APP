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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DumbbellApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "workout"

    val activity = LocalActivity.current as ComponentActivity           // ← 转成 ViewModelStoreOwner
    val wifiVm = viewModel<WifiScanViewModel>(viewModelStoreOwner = activity)

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
