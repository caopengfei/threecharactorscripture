package com.rakor.sanzijing.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.rakor.sanzijing.data.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsRepositoryTest {
    private fun newRepo(): SettingsRepository {
        val dir = File(System.getProperty("java.io.tmpdir"), "szj-${System.nanoTime()}").apply { mkdirs() }
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        ) { File(dir, "settings.preferences_pb") }
        return SettingsRepository(store)
    }

    @Test
    fun `默认值 自动朗读开 连读关 拼音开`() = runTest {
        val s = newRepo().settings.first()
        assertTrue(s.autoPlay)
        assertFalse(s.continuousPlay)
        assertTrue(s.showPinyin)
    }

    @Test
    fun `修改设置持久生效`() = runTest {
        val repo = newRepo()
        repo.setContinuousPlay(true)
        repo.setShowPinyin(false)
        val s = repo.settings.first()
        assertTrue(s.continuousPlay)
        assertFalse(s.showPinyin)
        assertTrue(s.autoPlay)
    }
}
