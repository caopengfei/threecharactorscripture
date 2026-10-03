package com.rakor.sanzijing

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.rakor.sanzijing.data.db.AppDatabase
import com.rakor.sanzijing.data.progress.ProgressRepository

class AppContainer(appContext: Context) {
    private val db: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "sanzijing.db").build()
    }
    val progressRepository: ProgressRepository by lazy {
        ProgressRepository(db)
    }
}

class SanzijingApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
