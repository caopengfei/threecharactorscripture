package com.rakor.sanzijing.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ReaderSettings(
    val autoPlay: Boolean = true,
    val continuousPlay: Boolean = false,
    val showPinyin: Boolean = true,
)

class SettingsRepository(private val store: DataStore<Preferences>) {
    private val autoPlay = booleanPreferencesKey("auto_play")
    private val continuousPlay = booleanPreferencesKey("continuous_play")
    private val showPinyin = booleanPreferencesKey("show_pinyin")

    val settings: Flow<ReaderSettings> = store.data.map { p ->
        ReaderSettings(
            autoPlay = p[autoPlay] ?: true,
            continuousPlay = p[continuousPlay] ?: false,
            showPinyin = p[showPinyin] ?: true,
        )
    }

    suspend fun setAutoPlay(v: Boolean) { store.edit { it[autoPlay] = v } }
    suspend fun setContinuousPlay(v: Boolean) { store.edit { it[continuousPlay] = v } }
    suspend fun setShowPinyin(v: Boolean) { store.edit { it[showPinyin] = v } }
}
