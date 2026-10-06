package com.kleos.sakshi.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kleos.sakshi.data.entities.DataGapEntity
import com.kleos.sakshi.data.entities.ListenerSessionEntity
import com.kleos.sakshi.data.entities.NotifEventEntity
import com.kleos.sakshi.data.entities.RawEventEntity

// DAOs hold queries only. Rules (dedupe key, half-open ranges) are in the SQL; meaning lives in the stores and the engine.

@Dao
interface RawEventDao {
    /** IGNORE swallows a row that repeats (ts, type, pkg). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(events: List<RawEventEntity>)

    @Query("SELECT * FROM raw_event WHERE ts >= :from AND ts < :to ORDER BY ts, id")
    fun range(from: Long, to: Long): List<RawEventEntity>

    @Query("SELECT MIN(ts) FROM raw_event")
    fun oldest(): Long?

    @Query("SELECT COUNT(*) FROM raw_event")
    fun count(): Int

    @Query("DELETE FROM raw_event WHERE ts < :ts")
    fun purgeBefore(ts: Long): Int
}

@Dao
interface NotifEventDao {
    @Insert
    fun insert(event: NotifEventEntity)

    @Query("SELECT * FROM notif_event WHERE ts >= :from AND ts < :to ORDER BY ts, id")
    fun range(from: Long, to: Long): List<NotifEventEntity>

    @Query("SELECT COUNT(*) FROM notif_event")
    fun count(): Int

    @Query("DELETE FROM notif_event WHERE ts < :ts")
    fun purgeBefore(ts: Long): Int
}

@Dao
interface ListenerSessionDao {
    @Insert
    fun insert(session: ListenerSessionEntity)

    /** Sessions that touch [from, to): started before `to` and not ended at or before `from`. */
    @Query(
        "SELECT * FROM listener_session WHERE connectedAt < :to AND (disconnectedAt IS NULL OR disconnectedAt > :from) " +
            "ORDER BY connectedAt, id")
    fun overlapping(from: Long, to: Long): List<ListenerSessionEntity>

    @Query("SELECT COUNT(*) FROM listener_session WHERE disconnectedAt IS NULL")
    fun openCount(): Int

    /** An end before the connect time is raised to the connect time, so a session never has a negative length. */
    @Query(
        "UPDATE listener_session SET disconnectedAt = CASE WHEN :at < connectedAt THEN connectedAt ELSE :at END " +
            "WHERE disconnectedAt IS NULL")
    fun closeOpen(at: Long): Int
}

@Dao
interface DataGapDao {
    @Insert
    fun insert(gap: DataGapEntity)

    /** PAUSED gaps only: a NOT_SEEN gap is never open. */
    @Query("UPDATE data_gap SET `end` = :at WHERE kind = 'PAUSED' AND `end` IS NULL")
    fun closeOpenPause(at: Long): Int

    @Query("SELECT * FROM data_gap WHERE start < :to AND (`end` IS NULL OR `end` > :from) ORDER BY start, id")
    fun overlapping(from: Long, to: Long): List<DataGapEntity>
}
