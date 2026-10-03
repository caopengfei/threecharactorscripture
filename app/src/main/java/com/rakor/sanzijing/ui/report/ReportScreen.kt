package com.rakor.sanzijing.ui.report

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.tv.material3.Text

/** Task 13 用完整学习报告页替换此占位。 */
@Composable
fun ReportScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("学习报告 · TODO")
    }
}
