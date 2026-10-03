package com.rakor.sanzijing.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.tv.material3.MaterialTheme

@Composable
fun SanzijingTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(CreamTop, CreamMid, CreamBottom)))
        ) { content() }
    }
}
