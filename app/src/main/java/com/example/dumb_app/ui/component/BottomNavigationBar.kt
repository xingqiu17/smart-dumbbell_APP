package com.example.dumb_app.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun BottomNavigationBar(selected: String, onSelect: (String) -> Unit) {
    val items = listOf(
        TabItem("workout", "运动", Icons.Default.FitnessCenter),
        TabItem("record", "记录", Icons.Default.History),
        TabItem("profile", "个人", Icons.Default.AccountCircle)
    )
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, null) },
                label = { Text(item.label) },
                selected = item.route == selected,
                onClick = { onSelect(item.route) }
            )
        }
    }
}
