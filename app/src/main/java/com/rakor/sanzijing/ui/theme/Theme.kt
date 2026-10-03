package com.rakor.sanzijing.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@Composable
fun SanzijingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Coral,
            onPrimary = Color.White,
            secondary = Honey,
            onSecondary = Color.White,
            surface = CardWhite,
            onSurface = InkBrown,
            background = CreamTop,
            onBackground = InkBrown,
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(CreamTop, CreamMid, CreamBottom)))
        ) { content() }
    }
}
