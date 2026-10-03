package com.rakor.sanzijing.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rakor.sanzijing.TestFiles
import com.rakor.sanzijing.data.content.ContentRepository
import com.rakor.sanzijing.data.db.AppDatabase
import com.rakor.sanzijing.data.progress.ProgressRepository
import com.rakor.sanzijing.ui.home.HomeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.FileInputStream
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private lateinit var db: AppDatabase
    private lateinit var vm: HomeViewModel
    private lateinit var scope: TestScope
    private lateinit var progress: ProgressRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val content = ContentRepository { FileInputStream(TestFiles.moduleFile("src/main/assets/content/sanzijing.json")) }
        progress = ProgressRepository(db, clock = { 1L })
        scope = TestScope(StandardTestDispatcher())
        vm = HomeViewModel(content, progress, scope, today = { LocalDate.of(2026, 10, 3) })
    }

    @After
    fun tearDown() { db.close() }

    private fun awaitReady() {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            scope.runCurrent()
            if (vm.state.value.ready) return
            Thread.sleep(20)
        }
        error("not ready")
    }

    @Test
    fun `空进度时继续学习为 u001 且章节进度为零`() {
        awaitReady()
        val s = vm.state.value
        assertEquals("u001", s.continueUnitId)
        assertEquals("人之初，性本善", s.continueText)
        assertEquals("教育之要", s.continueChapterTitle)
        assertEquals(0, s.readCount)
        assertEquals(192, s.totalCount)
        assertEquals(6, s.chapters.size)
        assertEquals(14, s.chapters[0].total)
        assertEquals(60, s.chapters[3].total)
    }

    @Test
    fun `标记已读后继续学习前移并统计`() {
        awaitReady()
        runBlocking { progress.onPlaybackCompleted("u001"); progress.onPlaybackCompleted("u002") }
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            scope.runCurrent()
            if (vm.state.value.continueUnitId == "u003") break
            Thread.sleep(20)
        }
        val s = vm.state.value
        assertEquals("u003", s.continueUnitId)
        assertEquals(2, s.readCount)
        assertEquals(2, s.chapters[0].read)
    }
}
