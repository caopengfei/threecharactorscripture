package com.rakor.sanzijing.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ProgressBarSimple(fraction: Float, barColor: Color, trackColor: Color, height: Dp = 8.dp, modifier: Modifier = Modifier) {
    val f = if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(trackColor, RoundedCornerShape(height / 2))
    ) {
        Box(
            Modifier
                .fillMaxWidth(f)
                .height(height)
                .background(barColor, RoundedCornerShape(height / 2))
        )
    }
}
