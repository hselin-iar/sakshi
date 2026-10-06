package com.kleos.sakshi.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kleos.sakshi.engine.model.AppMeta
import com.kleos.sakshi.engine.model.Baseline
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Experiment
import com.kleos.sakshi.engine.model.GoalAnswer
import com.kleos.sakshi.engine.model.GoalTap
import com.kleos.sakshi.engine.model.IngestState
import com.kleos.sakshi.engine.model.LakeRow
import com.kleos.sakshi.engine.model.LakeState
import com.kleos.sakshi.engine.model.NoteState
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.SayingPick
import com.kleos.sakshi.engine.model.Settings
import com.kleos.sakshi.engine.model.StartReason
import com.kleos.sakshi.engine.model.StudyBlock
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.SuggestionState
import com.kleos.sakshi.engine.model.TargetMetric
import com.kleos.sakshi.engine.model.UserClass
import com.kleos.sakshi.engine.model.Verdict
import com.kleos.sakshi.engine.model.WeekStart

private fun StudyDay?.epochDayOrNull() = this?.epochDay
private fun Long?.toWeekStart() = this?.let { WeekStart(StudyDay(it)) }
private fun Long?.toEpochMs() = this?.let(::EpochMs)

/** One row, id = 0. */
@Entity(tableName = "ingest_state")
data class IngestStateEntity(
    @PrimaryKey val id: Int = 0,
    val cursor: Long?, val lastRunAt: Long?, val paused: Boolean, val pausedSince: Long?, val firstReadAt: Long?,
    val lastWorkerRunAt: Long?, val workerRuns7d: Int, val lastError: String?, val oddEventPairs: Int) {
    fun toModel() = IngestState(
        cursor.toEpochMs(), lastRunAt.toEpochMs(), paused, pausedSince.toEpochMs(), firstReadAt.toEpochMs(),
        lastWorkerRunAt.toEpochMs(), workerRuns7d, lastError, oddEventPairs)
}

fun IngestState.toEntity() = IngestStateEntity(
    cursor = cursor?.value, lastRunAt = lastRunAt?.value, paused = paused, pausedSince = pausedSince?.value,
    firstReadAt = firstReadAt?.value, lastWorkerRunAt = lastWorkerRunAt?.value, workerRuns7d = workerRuns7d,
    lastError = lastError, oddEventPairs = oddEventPairs)

@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey val pkg: String,
    val label: String, val systemCategory: Int?, val userClass: UserClass, val addedAt: Long) {
    fun toModel() = AppMeta(Pkg(pkg), label, systemCategory, userClass, EpochMs(addedAt))
}

fun AppMeta.toEntity() = AppMetaEntity(pkg.value, label, systemCategory, userClass, addedAt.value)

/** One row, id = 0. Study blocks are stored as "start-end,start-end" minutes from local midnight. */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 0,
    val studyBlocks: String, val learnStudyHours: Boolean, val gentleMode: Boolean, val gentleExplicit: Boolean,
    val weeklyNoteEnabled: Boolean, val ageUnder18: Boolean, val batteryHelperShown: Boolean,
    val lapseAcknowledgedThrough: Long?, val firstReadAt: Long?, val createdAt: Long) {
    fun toModel() = Settings(
        studyBlocks = decodeBlocks(studyBlocks), learnStudyHours = learnStudyHours, gentleMode = gentleMode,
        gentleExplicit = gentleExplicit, weeklyNoteEnabled = weeklyNoteEnabled, ageUnder18 = ageUnder18,
        batteryHelperShown = batteryHelperShown, lapseAcknowledgedThrough = lapseAcknowledgedThrough?.let(::StudyDay),
        firstReadAt = firstReadAt.toEpochMs(), createdAt = EpochMs(createdAt))
}

fun Settings.toEntity() = SettingsEntity(
    studyBlocks = encodeBlocks(studyBlocks), learnStudyHours = learnStudyHours, gentleMode = gentleMode,
    gentleExplicit = gentleExplicit, weeklyNoteEnabled = weeklyNoteEnabled, ageUnder18 = ageUnder18,
    batteryHelperShown = batteryHelperShown, lapseAcknowledgedThrough = lapseAcknowledgedThrough.epochDayOrNull(),
    firstReadAt = firstReadAt?.value, createdAt = createdAt.value)

internal fun encodeBlocks(blocks: List<StudyBlock>) = blocks.joinToString(",") { "${it.startMinute}-${it.endMinute}" }
internal fun decodeBlocks(s: String): List<StudyBlock> =
    if (s.isEmpty()) emptyList() else s.split(',').map { b -> b.split('-').let { StudyBlock(it[0].toInt(), it[1].toInt()) } }

@Entity(tableName = "baseline")
data class BaselineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val frozenAt: Long, val c0: Double, val p0: Double, val r0: Double, val q0: Double, val daysUsed: Int, val isActive: Boolean) {
    fun toModel() = Baseline(id, EpochMs(frozenAt), c0, p0, r0, q0, daysUsed, isActive)
}

fun Baseline.toEntity() = BaselineEntity(id, frozenAt.value, c0, p0, r0, q0, daysUsed, isActive)

/** `subject` is "" when the suggestion has no subject package (a nullable column cannot sit in a primary key). */
@Entity(tableName = "suggestion_state", primaryKeys = ["kind", "subject"])
data class SuggestionStateEntity(
    val kind: SuggestionKind, val subject: String, val firstEligibleAt: Long?, val lastShownAt: Long?,
    val shownInWeek: Long?, val dismissedUntil: Long?, val retiredUntil: Long?, val status: String) {
    fun toModel() = SuggestionState(
        kind, subject.ifEmpty { null }?.let(::Pkg), firstEligibleAt.toEpochMs(), lastShownAt.toEpochMs(),
        shownInWeek.toWeekStart(), dismissedUntil.toEpochMs(), retiredUntil.toEpochMs(), status)
}

fun SuggestionState.toEntity() = SuggestionStateEntity(
    kind, subject?.value ?: "", firstEligibleAt?.value, lastShownAt?.value, shownInWeek?.studyDay?.epochDay,
    dismissedUntil?.value, retiredUntil?.value, status)

@Entity(tableName = "experiment")
data class ExperimentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: SuggestionKind, val subject: String?, val startedAt: Long, val startReason: StartReason, val target: TargetMetric,
    val beforeValue: Double?, val afterValue: Double?, val windowEnd: Long, val verdict: Verdict, val approxMix: Boolean,
    val shown: Boolean) {
    fun toModel() = Experiment(
        id, kind, subject?.let(::Pkg), EpochMs(startedAt), startReason, target, beforeValue, afterValue, EpochMs(windowEnd),
        verdict, approxMix, shown)
}

fun Experiment.toEntity() = ExperimentEntity(
    id, kind, subject?.value, startedAt.value, startReason, target, beforeValue, afterValue, windowEnd.value, verdict,
    approxMix, shown)

@Entity(tableName = "saying_pick")
data class SayingPickEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sayingId: String, val pickedAt: Long) {
    fun toModel() = SayingPick(id, sayingId, EpochMs(pickedAt))
}

fun SayingPick.toEntity() = SayingPickEntity(id, sayingId, pickedAt.value)

@Entity(tableName = "goal_tap")
data class GoalTapEntity(@PrimaryKey val weekStart: Long, val answer: GoalAnswer) {
    fun toModel() = GoalTap(WeekStart(StudyDay(weekStart)), answer)
}

fun GoalTap.toEntity() = GoalTapEntity(weekStart.studyDay.epochDay, answer)

/** One row, id = 0. */
@Entity(tableName = "lake_state")
data class LakeRowEntity(@PrimaryKey val id: Int = 0, val state: LakeState, val phrase: String, val asOf: Long?) {
    fun toModel() = LakeRow(state, phrase, asOf.toEpochMs())
}

fun LakeRow.toEntity() = LakeRowEntity(state = state, phrase = phrase, asOf = asOf?.value)

/** One row, id = 0. */
@Entity(tableName = "note_state")
data class NoteStateEntity(
    @PrimaryKey val id: Int = 0, val lastNoteWeek: Long?, val mirrorReadyWeek: Long?, val mirrorViewedWeek: Long?) {
    fun toModel() = NoteState(lastNoteWeek.toWeekStart(), mirrorReadyWeek.toWeekStart(), mirrorViewedWeek.toWeekStart())
}

fun NoteState.toEntity() = NoteStateEntity(
    lastNoteWeek = lastNoteWeek?.studyDay?.epochDay, mirrorReadyWeek = mirrorReadyWeek?.studyDay?.epochDay,
    mirrorViewedWeek = mirrorViewedWeek?.studyDay?.epochDay)
