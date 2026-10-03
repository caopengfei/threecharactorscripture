package com.rakor.sanzijing.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rakor.sanzijing.TestFiles
import com.rakor.sanzijing.data.content.ContentRepository
import com.rakor.sanzijing.data.db.AppDatabase
import com.rakor.sanzijing.data.progress.ProgressRepository
import com.rakor.sanzijing.ui.report.ReportViewModel
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
class ReportViewModelTest {
    private lateinit var db: AppDatabase
    private lateinit var vm: ReportViewModel
    private lateinit var scope: TestScope
    private lateinit var progress: ProgressRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        val content = ContentRepository { FileInputStream(TestFiles.moduleFile("src/main/assets/content/sanzijing.json")) }
        progress = ProgressRepository(db, clock = { 1L }, today = { LocalDate.of(2026, 10, 3) })
        scope = TestScope(StandardTestDispatcher())
        vm = ReportViewModel(content, progress, scope, today = { LocalDate.of(2026, 10, 3) })
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

    private fun awaitTotalRead(n: Int) {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            scope.runCurrent()
            if (vm.state.value.totalRead == n) return
            Thread.sleep(20)
        }
        error("totalRead never reached $n")
    }

    @Test
    fun `空进度时统计归零且总数为 192`() {
        awaitReady()
        val s = vm.state.value
        assertEquals(0, s.totalRead)
        assertEquals(192, s.totalCount)
        assertEquals(0, s.masteredCount)
        assertEquals(0, s.streakDays)
        assertEquals(6, s.chapters.size)
    }

    @Test
    fun `已读与熟读分别计入总量与章节`() {
        awaitReady()
        runBlocking { progress.onPlaybackCompleted("u001"); progress.toggleMastered("u002") }
        awaitTotalRead(2)
        val s = vm.state.value
        assertEquals(2, s.totalRead)
        assertEquals(1, s.masteredCount)
        assertEquals(2, s.chapters[0].read)
        assertEquals(1, s.chapters[0].mastered)
        assertEquals(0, s.chapters[3].read)
    }
}
