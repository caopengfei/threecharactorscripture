package com.rakor.sanzijing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rakor.sanzijing.ui.navigation.AppNavGraph
import com.rakor.sanzijing.ui.theme.SanzijingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SanzijingTheme {
                AppNavGraph(app = applicationContext as SanzijingApp)
            }
        }
    }
}
