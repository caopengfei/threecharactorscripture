package com.rakor.sanzijing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rakor.sanzijing.ui.common.PinyinPhrase
import com.rakor.sanzijing.ui.theme.SanzijingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SanzijingTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PinyinPhrase("人之初", "rén zhī chū", showPinyin = true)
                }
            }
        }
    }
}
