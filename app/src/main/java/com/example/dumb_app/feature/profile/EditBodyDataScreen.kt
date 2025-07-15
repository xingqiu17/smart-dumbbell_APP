// app/src/main/java/com/example/dumb_app/feature/profile/EditBodyDataScreen.kt

package com.example.dumb_app.feature.profile

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.LocalDate
import java.time.Period
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBodyDataScreen(navController: NavController) {
    val context = LocalContext.current

    var birthDate by remember { mutableStateOf(LocalDate.of(1990,1,1)) }
    var showPicker by remember { mutableStateOf(false) }
    val age = Period.between(birthDate, LocalDate.now()).years

    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    val genders = listOf("男","女","其他")
    var genderExpanded by remember { mutableStateOf(false) }
    var gender by remember { mutableStateOf(genders.first()) }

    if (showPicker) {
        DatePickerDialog(
            context,
            { _, y, m, d ->
                birthDate = LocalDate.of(y, m+1, d)
                showPicker = false
            },
            birthDate.year, birthDate.monthValue-1, birthDate.dayOfMonth
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("修改身体数据") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 出生日期
            OutlinedTextField(
                value = birthDate.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("出生日期") },
                trailingIcon = { Text("年龄 $age 岁") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPicker = true }
            )
            // 身高
            OutlinedTextField(
                value = height,
                onValueChange = { height = it },
                label = { Text("身高 (cm)") },
                modifier = Modifier.fillMaxWidth()
            )
            // 体重
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("体重 (kg)") },
                modifier = Modifier.fillMaxWidth()
            )
            // 性别
            ExposedDropdownMenuBox(
                expanded = genderExpanded,
                onExpandedChange = { genderExpanded = it }
            ) {
                OutlinedTextField(
                    value = gender,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("性别") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false }
                ) {
                    genders.forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = {
                                gender = it
                                genderExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("birthDate", birthDate.toString())
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("height", height.toIntOrNull() ?: 0)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("weight", weight.toIntOrNull() ?: 0)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("gender", gender)
                    navController.popBackStack()
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("保存")
            }
        }
    }
}
