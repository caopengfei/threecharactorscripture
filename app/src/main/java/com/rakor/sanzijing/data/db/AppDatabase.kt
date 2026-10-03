package com.rakor.sanzijing.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UnitProgressEntity::class, DailyStatsEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun unitProgressDao(): UnitProgressDao
    abstract fun dailyStatsDao(): DailyStatsDao
}
