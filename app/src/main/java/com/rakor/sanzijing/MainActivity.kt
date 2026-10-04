package com.rakor.sanzijing

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rakor.sanzijing.ui.navigation.AppNavGraph
import com.rakor.sanzijing.ui.theme.SanzijingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 亲子共学需要长时间停留阅读，全程保持屏幕常亮，避免触发 TV 系统屏保/息屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            SanzijingTheme {
                AppNavGraph(app = applicationContext as SanzijingApp)
            }
        }
    }
}
