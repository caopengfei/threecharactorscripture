package com.rakor.sanzijing

import android.app.Application
import android.content.Context

class AppContainer(private val appContext: Context)

class SanzijingApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
