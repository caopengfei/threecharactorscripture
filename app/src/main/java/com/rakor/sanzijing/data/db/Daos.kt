package com.rakor.sanzijing.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitProgressDao {
    @Query("SELECT * FROM unit_progress")
    fun observeAll(): Flow<List<UnitProgressEntity>>

    @Query("SELECT * FROM unit_progress")
    suspend fun observeAllBlocking(): List<UnitProgressEntity>

    @Query("SELECT * FROM unit_progress WHERE unitId = :unitId")
    suspend fun get(unitId: String): UnitProgressEntity?

    @Query("SELECT * FROM unit_progress ORDER BY lastReadAt DESC LIMIT 1")
    suspend fun lastRead(): UnitProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UnitProgressEntity)

    @Query("DELETE FROM unit_progress")
    suspend fun clearAll()
}

@Dao
interface DailyStatsDao {
    @Query("SELECT * FROM daily_stats ORDER BY date")
    fun observeAll(): Flow<List<DailyStatsEntity>>

    @Query("SELECT * FROM daily_stats")
    suspend fun observeAllBlocking(): List<DailyStatsEntity>

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun get(date: String): DailyStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyStatsEntity)

    @Query("DELETE FROM daily_stats")
    suspend fun clearAll()
}
