package com.rakor.sanzijing.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unit_progress")
data class UnitProgressEntity(
    @PrimaryKey val unitId: String,
    val status: Int,          // 0 未学 / 1 已读 / 2 已熟读
    val readCount: Int,
    val lastReadAt: Long,
    val markedAt: Long?,
)

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey val date: String,   // yyyy-MM-dd
    val secondsStudied: Long,
    val unitsRead: Int,
)
