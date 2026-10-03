package com.rakor.sanzijing

import android.app.Application
import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.rakor.sanzijing.data.audio.AudioPlayer
import com.rakor.sanzijing.data.audio.MediaPlayerAudioPlayer
import com.rakor.sanzijing.data.content.ContentRepository
import com.rakor.sanzijing.data.db.AppDatabase
import com.rakor.sanzijing.data.prefs.SettingsRepository
import com.rakor.sanzijing.data.progress.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(appContext: Context) {
    private val db: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "sanzijing.db").build()
    }
    val progressRepository: ProgressRepository by lazy {
        ProgressRepository(db)
    }
    val contentRepository: ContentRepository by lazy {
        ContentRepository { appContext.assets.open(it) }
    }
    val settingsRepository: SettingsRepository by lazy {
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        ) { appContext.preferencesDataStoreFile("settings") }
        SettingsRepository(store)
    }
    val audioPlayer: AudioPlayer by lazy { MediaPlayerAudioPlayer(appContext.assets) }
}

class SanzijingApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
