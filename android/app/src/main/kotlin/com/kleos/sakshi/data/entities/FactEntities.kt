package com.kleos.sakshi.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kleos.sakshi.engine.model.DataGap
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.GapKind
import com.kleos.sakshi.engine.model.ListenerSession
import com.kleos.sakshi.engine.model.NotifEvent
import com.kleos.sakshi.engine.model.NotifKind
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.RawEvent
import com.kleos.sakshi.engine.model.RawType
import com.kleos.sakshi.engine.model.RemovalKind

// Room entities mirror LC-1/LC-2 field for field. Enums are stored by symbolic name.

/**
 * `pkg` is "" for screen and keyguard events. SQLite treats NULLs as distinct inside a unique index, so a nullable
 * column would let duplicate screen events through; the empty string keeps the (ts, type, pkg) key honest.
 */
@Entity(
    tableName = "raw_event",
    indices = [Index(value = ["ts", "type", "pkg"], unique = true), Index(value = ["ts"])])
data class RawEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long, val type: RawType, val pkg: String) {
    fun toModel() = RawEvent(EpochMs(ts), type, pkg.ifEmpty { null }?.let(::Pkg))
}

fun RawEvent.toEntity() = RawEventEntity(ts = ts.value, type = type, pkg = pkg?.value ?: "")

@Entity(tableName = "notif_event", indices = [Index(value = ["pkg", "ts"]), Index(value = ["ts"])])
data class NotifEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long, val pkg: String, val category: String?, val kind: NotifKind, val removal: RemovalKind?, val ongoing: Boolean) {
    fun toModel() = NotifEvent(EpochMs(ts), Pkg(pkg), category, kind, removal, ongoing)
}

fun NotifEvent.toEntity() = NotifEventEntity(
    ts = ts.value, pkg = pkg.value, category = category, kind = kind, removal = removal, ongoing = ongoing)

@Entity(tableName = "listener_session", indices = [Index(value = ["connectedAt"])])
data class ListenerSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val connectedAt: Long, val disconnectedAt: Long?) {
    fun toModel() = ListenerSession(EpochMs(connectedAt), disconnectedAt?.let(::EpochMs))
}

@Entity(tableName = "data_gap", indices = [Index(value = ["start"])])
data class DataGapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: GapKind, val start: Long, val end: Long?) {
    fun toModel() = DataGap(kind, EpochMs(start), end?.let(::EpochMs))
}

fun DataGap.toEntity() = DataGapEntity(kind = kind, start = start.value, end = end?.value)
