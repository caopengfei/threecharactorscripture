package com.rakor.sanzijing.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rakor.sanzijing.ui.theme.BodyStyle
import com.rakor.sanzijing.ui.theme.ToastBrown
import kotlinx.coroutines.delay

@Composable
fun ToastHost(toast: String?, onConsumed: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2500)
            onConsumed()
        }
    }
    if (toast != null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier
                    .background(ToastBrown, RoundedCornerShape(24.dp))
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Text(toast, style = BodyStyle.copy(color = Color.White))
            }
        }
    }
}
