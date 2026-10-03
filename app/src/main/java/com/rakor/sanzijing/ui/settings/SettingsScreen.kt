package com.rakor.sanzijing.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.tv.material3.Text
import com.rakor.sanzijing.SanzijingApp

/** Task 13 用完整设置页替换此占位。 */
@Composable
fun SettingsScreen(app: SanzijingApp, onBack: () -> Unit) {
    BackHandler { onBack() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("设置 · TODO")
    }
}
