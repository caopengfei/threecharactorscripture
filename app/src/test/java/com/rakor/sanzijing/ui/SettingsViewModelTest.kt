package com.rakor.sanzijing.ui

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rakor.sanzijing.data.db.AppDatabase
import com.rakor.sanzijing.data.prefs.SettingsRepository
import com.rakor.sanzijing.data.progress.ProgressRepository
import com.rakor.sanzijing.ui.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private lateinit var db: AppDatabase
    private lateinit var vm: SettingsViewModel
    private lateinit var scope: TestScope
    private lateinit var progress: ProgressRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        progress = ProgressRepository(db, clock = { 1L }, today = { LocalDate.of(2026, 10, 3) })
        val dir = File(System.getProperty("java.io.tmpdir"), "szj-${System.nanoTime()}").apply { mkdirs() }
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        ) { File(dir, "settings.preferences_pb") }
        val settings = SettingsRepository(store)
        scope = TestScope(StandardTestDispatcher())
        vm = SettingsViewModel(settings, progress, scope)
    }

    @After
    fun tearDown() { db.close() }

    /** DataStore 读写走自有 IO 作用域 + 测试调度器，轮询直到条件满足。 */
    private fun awaitSetting(predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            scope.runCurrent()
            if (predicate()) return
            Thread.sleep(20)
        }
        error("setting condition not met")
    }

    @Test
    fun `默认设置 自动朗读开 连读关 拼音开`() {
        awaitSetting { vm.state.value.settings.showPinyin }   // 等首次 DataStore 发射
        val s = vm.state.value.settings
        assertTrue(s.autoPlay)
        assertFalse(s.continuousPlay)
        assertTrue(s.showPinyin)
    }

    @Test
    fun `开关切换并写回状态`() {
        awaitSetting { vm.state.value.settings.showPinyin }
        vm.toggleContinuous()
        vm.togglePinyin()
        awaitSetting { vm.state.value.settings.continuousPlay && !vm.state.value.settings.showPinyin }
        assertTrue(vm.state.value.settings.continuousPlay)
        assertFalse(vm.state.value.settings.showPinyin)
        assertTrue(vm.state.value.settings.autoPlay)
    }

    @Test
    fun `清除进度对话框开关状态`() {
        vm.requestClearDialog(true)
        assertTrue(vm.state.value.showClearDialog)
        vm.requestClearDialog(false)
        assertFalse(vm.state.value.showClearDialog)
    }

    @Test
    fun `确认清除 清空数据库并提示成功`() {
        runBlocking { progress.onPlaybackCompleted("u001") }
        vm.requestClearDialog(true)
        vm.confirmClear()
        assertFalse(vm.state.value.showClearDialog)   // 对话框乐观关闭
        awaitSetting { vm.state.value.message == "学习进度已清除" }   // 成功提示仅在 clearAll 完成后
        runBlocking {
            assertTrue(db.unitProgressDao().observeAllBlocking().isEmpty())
            assertTrue(db.dailyStatsDao().observeAllBlocking().isEmpty())
        }
        vm.consumeMessage()
        assertEquals(null, vm.state.value.message)
    }
}
