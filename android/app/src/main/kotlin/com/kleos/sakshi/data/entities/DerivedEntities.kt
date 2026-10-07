package com.kleos.sakshi.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.EndedBy
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pattern
import com.kleos.sakshi.engine.model.PatternKind
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.Shape
import com.kleos.sakshi.engine.model.Stay
import com.kleos.sakshi.engine.model.Stretch
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.WeekStart
import com.kleos.sakshi.engine.model.WeekSummary
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.model.WindowSource
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Entity(tableName = "day_summary")
data class DaySummaryEntity(
    @PrimaryKey val day: Long,
    val valid: Boolean, val windowMinutes: Double, val quietMinutes: Double, val inSetMinutes: Double, val coverage: Double,
    val pickups: Int, val switchesPerHour: Double?, val flinch: Boolean?, val rampUpMin: Double?, val lastScreenOffTs: Long?,
    val firstStretchMin: Double?, val externalResumes: Int) {
    fun toModel() = DaySummary(
        StudyDay(day), valid, windowMinutes, quietMinutes, inSetMinutes, coverage, pickups, switchesPerHour, flinch,
        rampUpMin, lastScreenOffTs?.let(::EpochMs), firstStretchMin, externalResumes)
}

fun DaySummary.toEntity() = DaySummaryEntity(
    day.epochDay, valid, windowMinutes, quietMinutes, inSetMinutes, coverage, pickups, switchesPerHour, flinch, rampUpMin,
    lastScreenOffTs?.value, firstStretchMin, externalResumes)

/**
 * The id comes from the engine and is kept as given; 0 lets Room assign one.
 * `quietMinutes` and `glances` are the WindowWithDetail fields (LC-2) that the Window entity does not carry,
 * stored here so a stored window reads back as the same WindowWithDetail.
 */
@Entity(tableName = "window", indices = [Index(value = ["day"]), Index(value = ["start"])])
data class WindowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long, val start: Long, val end: Long, val source: WindowSource, val partial: Boolean, val finalised: Boolean,
    val shape: Shape?, val quietMinutes: Double, val glances: Int) {
    fun toModel() = Window(id, StudyDay(day), EpochMs(start), EpochMs(end), source, partial, finalised, shape)
}

fun Window.toEntity(quietMinutes: Double, glances: Int) = WindowEntity(
    id, day.epochDay, start.value, end.value, source, partial, finalised, shape, quietMinutes, glances)

@Entity(tableName = "stretch", indices = [Index(value = ["windowId"])])
data class StretchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val windowId: Long, val start: Long, val end: Long, val minutes: Double, val inSetMinutes: Double, val quietMinutes: Double,
    val endedBy: EndedBy) {
    fun toModel() = Stretch(id, windowId, EpochMs(start), EpochMs(end), minutes, inSetMinutes, quietMinutes, endedBy)
}

fun Stretch.toEntity() = StretchEntity(id, windowId, start.value, end.value, minutes, inSetMinutes, quietMinutes, endedBy)

@Entity(tableName = "stay", indices = [Index(value = ["windowId"])])
data class StayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val windowId: Long, val start: Long, val end: Long, val firstPkg: String, val pkgMain: String, val origin: Origin,
    val stonePkg: String?, val notifClicked: Boolean, val returnMinutes: Double?, val glancesBefore: Int) {
    fun toModel() = Stay(
        id, windowId, EpochMs(start), EpochMs(end), Pkg(firstPkg), Pkg(pkgMain), origin, stonePkg?.let(::Pkg), notifClicked,
        returnMinutes, glancesBefore)
}

fun Stay.toEntity() = StayEntity(
    id, windowId, start.value, end.value, firstPkg.value, pkgMain.value, origin, stonePkg?.value, notifClicked, returnMinutes,
    glancesBefore)

@Entity(tableName = "week_summary")
data class WeekSummaryEntity(
    @PrimaryKey val weekStart: Long,
    val stretchMedianMin: Double?, val longestStretchMin: Double?, val staysPerHour: Double?, val returnMedianMin: Double?,
    val quietShare: Double?, val inSetShare: Double?, val steadiness: Int?, val word: String?, val windowsCount: Int,
    val validDays: Int, val unusual: Boolean, val appOpens: Int, val appMinutes: Double, val returnsAfterLapse: Int) {
    fun toModel() = WeekSummary(
        WeekStart(StudyDay(weekStart)), stretchMedianMin, longestStretchMin, staysPerHour, returnMedianMin, quietShare,
        inSetShare, steadiness, word, windowsCount, validDays, unusual, appOpens, appMinutes, returnsAfterLapse)
}

fun WeekSummary.toEntity() = WeekSummaryEntity(
    weekStart.studyDay.epochDay, stretchMedianMin, longestStretchMin, staysPerHour, returnMedianMin, quietShare, inSetShare,
    steadiness, word, windowsCount, validDays, unusual, appOpens, appMinutes, returnsAfterLapse)

/** Upserted by (kind, key); the engine supplies firstSeen. `args` is stored as a JSON object of strings. */
@Entity(tableName = "pattern", primaryKeys = ["kind", "key"])
data class PatternEntity(
    val kind: PatternKind, val key: String, val strength: Double, val evidenceWindows: Int, val evidenceDays: Int,
    val firstSeen: Long, val lastSeen: Long, val args: String) {
    fun toModel() = Pattern(
        kind, key, strength, evidenceWindows, evidenceDays, EpochMs(firstSeen), EpochMs(lastSeen), decodeArgs(args))
}

fun Pattern.toEntity() = PatternEntity(
    kind, key, strength, evidenceWindows, evidenceDays, firstSeen.value, lastSeen.value, encodeArgs(args))

private val argsSerializer = MapSerializer(String.serializer(), String.serializer())
internal fun encodeArgs(args: Map<String, String>): String = Json.encodeToString(argsSerializer, args)
internal fun decodeArgs(s: String): Map<String, String> = Json.decodeFromString(argsSerializer, s)
