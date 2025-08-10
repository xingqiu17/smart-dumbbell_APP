// app/src/main/java/com/example/dumb_app/feature/workout/RestScreen.kt
package com.example.dumb_app.feature.workout

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dumb_app.core.connectivity.wifi.WifiScanViewModel
import kotlinx.coroutines.delay

private const val TAG_RS = "RestScreen"
private const val TRAINING_ROUTE = "training"

@Composable
fun RestScreen(
    navController: NavController,
    wifiVm: WifiScanViewModel,
    totalSeconds: Int
) {
    var secondsRemaining by remember { mutableStateOf(totalSeconds) }

    // ✅ 1. 将“跳过并进入下一组”的操作提取为公共函数
    val advanceToNextSet = {
        runCatching {
            val trainingEntry = navController.getBackStackEntry(TRAINING_ROUTE)
            trainingEntry.savedStateHandle.set("advanceSet", true)
        }.onFailure {
            Log.e(TAG_RS, "failed to set advanceSet on handle: ${it.message}", it)
        }
        navController.popBackStack()
    }

    // ✅ 2. 监听来自设备的 "rest_skipped" 事件
    val wsEvent by wifiVm.wsEvents.collectAsState(initial = null)
    LaunchedEffect(wsEvent) {
        if (wsEvent is WifiScanViewModel.WsEvent.RestSkipped) {
            Log.d(TAG_RS, "Received skip rest signal from device.")
            advanceToNextSet()      // 执行公共操作
            wifiVm.clearEvent()     // 消费事件
        }
    }

    // 倒计时逻辑保持不变
    LaunchedEffect(Unit) {
        Log.d(TAG_RS, "enter rest, totalSeconds=$totalSeconds")
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
            Log.d(TAG_RS, "tick: secondsRemaining=$secondsRemaining")
        }

        Log.d(TAG_RS, "countdown finished -> advancing to next set")
        advanceToNextSet() // 倒计时结束时也调用公共操作
    }

    val progress by animateFloatAsState(
        targetValue = secondsRemaining / totalSeconds.toFloat(),
        animationSpec = tween(durationMillis = 400, easing = LinearEasing)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // UI部分保持不变
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = progress,
                strokeWidth = 8.dp,
                modifier = Modifier.size(200.dp)
            )
            Text(
                text = secondsRemaining.toString(),
                style = MaterialTheme.typography.headlineLarge
            )
        }

        Spacer(Modifier.height(24.dp))

        // ✅ 3. 修改按钮点击事件，使其也调用公共函数
        Button(onClick = {
            Log.d(TAG_RS, "skip rest button clicked")
            // 首先通知设备，App执行了跳过操作
            wifiVm.sendSkipRest()
            // 然后执行UI跳转
            advanceToNextSet()
        }) {
            Text("跳过休息")
        }
    }
}