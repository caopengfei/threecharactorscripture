package com.rakor.sanzijing

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSmokeTest {
    @Test
    fun mainActivityLaunchesWithoutCrash() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { /* 渲染成功不崩溃即通过 */ }
        }
    }
}
