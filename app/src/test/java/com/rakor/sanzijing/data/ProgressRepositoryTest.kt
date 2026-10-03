package com.rakor.sanzijing.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rakor.sanzijing.data.db.AppDatabase
import com.rakor.sanzijing.data.db.DailyStatsEntity
import com.rakor.sanzijing.data.db.UnitProgressEntity
import com.rakor.sanzijing.data.progress.ProgressRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ProgressRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        var tick = 1_000_000L
        repo = ProgressRepository(
            db,
            clock = { ++tick },
            today = { LocalDate.of(2026, 10, 3) },
        )
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `播放完成标记已读并计数`() = runTest {
        repo.onPlaybackCompleted("u001")
        repo.onPlaybackCompleted("u001")
        val e = db.unitProgressDao().get("u001")!!
        assertEquals(2, e.readCount)
        assertEquals(ProgressRepository.STATUS_READ, e.status)
        assertEquals(1, db.dailyStatsDao().get("2026-10-03")!!.unitsRead)
    }

    @Test
    fun `长按标记熟读再按取消`() = runTest {
        repo.toggleMastered("u002")
        assertEquals(ProgressRepository.STATUS_MASTERED, db.unitProgressDao().get("u002")!!.status)
        repo.toggleMastered("u002")
        assertEquals(ProgressRepository.STATUS_READ, db.unitProgressDao().get("u002")!!.status)
    }

    @Test
    fun `继续学习返回第一个未学单元`() {
        val all = listOf("u001", "u002", "u003", "u004")
        assertEquals("u001", repo.continueUnitId(all, emptyList()))
        val p1 = UnitProgressEntity("u001", 1, 1, 100L, null)
        assertEquals("u002", repo.continueUnitId(all, listOf(p1)))
        val p2 = p1.copy(unitId = "u002", lastReadAt = 200L)
        assertEquals("u003", repo.continueUnitId(all, listOf(p1, p2)))
    }

    @Test
    fun `全部学完时继续学习回到最后互动单元`() {
        val all = listOf("u001", "u002")
        val p1 = UnitProgressEntity("u001", 1, 1, 100L, null)
        val p2 = UnitProgressEntity("u002", 2, 1, 300L, 300L)
        assertEquals("u002", repo.continueUnitId(all, listOf(p1, p2)))
    }

    @Test
    fun `学习时长按日累计`() = runTest {
        repo.addStudySeconds(30)
        repo.addStudySeconds(45)
        assertEquals(75L, db.dailyStatsDao().get("2026-10-03")!!.secondsStudied)
    }

    @Test
    fun `连续学习天数计算`() {
        fun day(s: Long) = DailyStatsEntity(date = "2026-10-%02d".format(s), secondsStudied = 60, unitsRead = 1)
        val stats = listOf(day(1), day(2), day(3))
        assertEquals(3, repo.studyStreak(stats, LocalDate.of(2026, 10, 3)))
        assertEquals(0, repo.studyStreak(stats, LocalDate.of(2026, 10, 5)))  // 断了
        assertEquals(3, repo.studyStreak(stats, LocalDate.of(2026, 10, 4)))  // 今天没学，但昨天连续
    }

    @Test
    fun `清除进度`() = runTest {
        repo.onPlaybackCompleted("u001")
        repo.clearAll()
        assertEquals(0, db.unitProgressDao().observeAllBlocking().size)
        assertEquals(0, db.dailyStatsDao().observeAllBlocking().size)
    }

    @Test
    fun `回放已熟读单元不降级且保留标记时间`() = runTest {
        repo.toggleMastered("u003")
        val before = db.unitProgressDao().get("u003")!!
        repo.onPlaybackCompleted("u003")
        val after = db.unitProgressDao().get("u003")!!
        assertEquals(ProgressRepository.STATUS_MASTERED, after.status)
        assertEquals(before.markedAt, after.markedAt)
        assertEquals(1, after.readCount)
    }
}
