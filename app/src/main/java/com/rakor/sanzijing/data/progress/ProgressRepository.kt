package com.rakor.sanzijing.data.progress

import androidx.room.withTransaction
import com.rakor.sanzijing.data.db.AppDatabase
import com.rakor.sanzijing.data.db.DailyStatsEntity
import com.rakor.sanzijing.data.db.UnitProgressEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class ProgressRepository(
    private val db: AppDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
    private val today: () -> LocalDate = LocalDate::now,
) {
    private val progressDao = db.unitProgressDao()
    private val statsDao = db.dailyStatsDao()

    fun observeProgress(): Flow<List<UnitProgressEntity>> = progressDao.observeAll()
    fun observeDailyStats(): Flow<List<DailyStatsEntity>> = statsDao.observeAll()

    /** 播放某单元音频完成：readCount+1，status 至少为已读；首次学完时当日 unitsRead+1。 */
    suspend fun onPlaybackCompleted(unitId: String) {
        val now = clock()
        val firstCompletion = db.withTransaction {
            val existing = progressDao.get(unitId)
            progressDao.upsert(
                UnitProgressEntity(
                    unitId = unitId,
                    status = maxOf(existing?.status ?: STATUS_NEW, STATUS_READ),
                    readCount = (existing?.readCount ?: 0) + 1,
                    lastReadAt = now,
                    markedAt = existing?.markedAt,
                )
            )
            existing == null || existing.status == STATUS_NEW
        }
        // 当日已学单元数只统计首次读完的单元，重复播放不重复计数
        if (firstCompletion) {
            val date = today().toString()
            statsDao.ensureRow(date)
            statsDao.incrementUnitsRead(date)
        }
    }

    /** 长按 OK：未熟读→熟读；已熟读→回退为已读。 */
    suspend fun toggleMastered(unitId: String) = db.withTransaction {
        val existing = progressDao.get(unitId)
            ?: UnitProgressEntity(unitId, STATUS_NEW, 0, clock(), null)
        val toMastered = existing.status != STATUS_MASTERED
        progressDao.upsert(
            existing.copy(
                status = if (toMastered) STATUS_MASTERED else STATUS_READ,
                markedAt = if (toMastered) clock() else null,
            )
        )
    }

    suspend fun addStudySeconds(seconds: Long) {
        val date = today().toString()
        statsDao.ensureRow(date)
        statsDao.addSeconds(date, seconds)
    }

    suspend fun lastInteractedUnitId(): String? = progressDao.lastRead()?.unitId

    suspend fun clearAll() = db.withTransaction {
        progressDao.clearAll()
        statsDao.clearAll()
    }

    /** 继续学习 = 全文顺序第一个未学单元；全学完则回到最后互动单元。 */
    fun continueUnitId(allUnitIds: List<String>, progress: List<UnitProgressEntity>): String {
        val read = progress.filter { it.status > STATUS_NEW }.map { it.unitId }.toSet()
        return allUnitIds.firstOrNull { it !in read }
            ?: progress.maxByOrNull { it.lastReadAt }?.unitId
            ?: allUnitIds.first()
    }

    /** 连续学习天数：今天已学从今天数，否则从昨天往前数。 */
    fun studyStreak(stats: List<DailyStatsEntity>, today: LocalDate = this.today()): Int {
        val days = stats.filter { it.secondsStudied > 0 || it.unitsRead > 0 }
            .map { LocalDate.parse(it.date) }.toSet()
        var d = if (today in days) today else today.minusDays(1)
        var streak = 0
        while (d in days) { streak++; d = d.minusDays(1) }
        return streak
    }

    companion object {
        const val STATUS_NEW = 0
        const val STATUS_READ = 1
        const val STATUS_MASTERED = 2
    }
}
