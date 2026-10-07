package com.kleos.sakshi.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kleos.sakshi.data.entities.DaySummaryEntity
import com.kleos.sakshi.data.entities.PatternEntity
import com.kleos.sakshi.data.entities.StayEntity
import com.kleos.sakshi.data.entities.StretchEntity
import com.kleos.sakshi.data.entities.WeekSummaryEntity
import com.kleos.sakshi.data.entities.WindowEntity

@Dao
interface DerivedDao {
    // writing a day
    @Query("DELETE FROM stretch WHERE windowId IN (SELECT id FROM `window` WHERE day = :day)")
    fun deleteStretchesOfDay(day: Long)

    @Query("DELETE FROM stay WHERE windowId IN (SELECT id FROM `window` WHERE day = :day)")
    fun deleteStaysOfDay(day: Long)

    @Query("DELETE FROM `window` WHERE day = :day")
    fun deleteWindowsOfDay(day: Long)

    @Query("DELETE FROM day_summary WHERE day = :day")
    fun deleteSummaryOfDay(day: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWindows(windows: List<WindowEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStretches(stretches: List<StretchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStays(stays: List<StayEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSummary(summary: DaySummaryEntity)

    // reading
    @Query("SELECT * FROM `window` WHERE start >= :from AND start < :to ORDER BY start, id")
    fun windowsStartingIn(from: Long, to: Long): List<WindowEntity>

    @Query("SELECT * FROM stretch WHERE windowId IN (:ids) ORDER BY start, id")
    fun stretchesOf(ids: List<Long>): List<StretchEntity>

    @Query("SELECT * FROM stay WHERE windowId IN (:ids) ORDER BY start, id")
    fun staysOf(ids: List<Long>): List<StayEntity>

    @Query("SELECT * FROM day_summary WHERE day >= :from AND day <= :to ORDER BY day")
    fun days(from: Long, to: Long): List<DaySummaryEntity>

    // weeks and patterns
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertWeek(week: WeekSummaryEntity)

    @Query("SELECT * FROM week_summary ORDER BY weekStart")
    fun weeks(): List<WeekSummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertPatterns(patterns: List<PatternEntity>)

    @Query("SELECT * FROM pattern ORDER BY kind, `key`")
    fun patterns(): List<PatternEntity>

    // clearing derived data (baseline and state are not derived and stay)
    @Query("DELETE FROM stretch") fun clearStretches()
    @Query("DELETE FROM stay") fun clearStays()
    @Query("DELETE FROM `window`") fun clearWindows()
    @Query("DELETE FROM day_summary") fun clearDaySummaries()
    @Query("DELETE FROM week_summary") fun clearWeekSummaries()
    @Query("DELETE FROM pattern") fun clearPatterns()
}
