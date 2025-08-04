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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

private const val TAG_RS = "RestScreen"

// ⚠️ 把这个改成你训练页在 NavGraph 中的 route 字符串
private const val TRAINING_ROUTE = "training"

@Composable
fun RestScreen(
    navController: NavController,
    totalSeconds: Int = 5
) {
    var secondsRemaining by remember { mutableStateOf(totalSeconds) }

    LaunchedEffect(Unit) {
        Log.d(TAG_RS, "enter rest, totalSeconds=$totalSeconds")
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
            Log.d(TAG_RS, "tick: secondsRemaining=$secondsRemaining")
        }

        // 直接拿“训练页”的 BackStackEntry，写入 advanceSet
        runCatching {
            val trainingEntry = navController.getBackStackEntry(TRAINING_ROUTE)
            Log.d(TAG_RS, "countdown finished -> set advanceSet=true on training handle, then pop")
            trainingEntry.savedStateHandle.set("advanceSet", true)
        }.onFailure {
            Log.e(TAG_RS, "failed to set advanceSet on training handle: ${it.message}", it)
        }

        navController.popBackStack() // 返回训练页
    }

    val progress by animateFloatAsState(
        targetValue = secondsRemaining / totalSeconds.toFloat(),
        animationSpec = tween(durationMillis = 400, easing = LinearEasing)
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
}
