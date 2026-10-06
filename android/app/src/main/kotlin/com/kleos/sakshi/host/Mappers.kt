package com.kleos.sakshi.host

import com.kleos.sakshi.engine.model.*
import com.kleos.sakshi.host.gen.*
import java.time.Instant
import java.time.ZoneId
import com.kleos.sakshi.engine.tuning.Tuning

/**
 * Engine views to Pigeon DTOs, field for field. No decisions here: nothing is computed, filtered or reworded.
 * Epoch times keep their value and gain an "EpochMs" suffix; Ints become Longs because Pigeon's int is 64-bit.
 */

// ---- enums ----
fun DataState.toDto() = when (this) {
    DataState.OK -> DataStateDto.OK
    DataState.LEARNING_BASELINE -> DataStateDto.LEARNING_BASELINE
    DataState.TOO_LITTLE_DATA -> DataStateDto.TOO_LITTLE_DATA
}

fun DataFlag.toDto() = when (this) {
    DataFlag.PING_OFF -> DataFlagDto.PING_OFF
    DataFlag.PARTIAL_PING -> DataFlagDto.PARTIAL_PING
    DataFlag.NOT_SEEN -> DataFlagDto.NOT_SEEN
    DataFlag.PAUSED -> DataFlagDto.PAUSED
    DataFlag.TOO_LITTLE_DATA -> DataFlagDto.TOO_LITTLE_DATA
    DataFlag.UNUSUAL_WEEK -> DataFlagDto.UNUSUAL_WEEK
    DataFlag.INTERNAL_PARTIAL -> DataFlagDto.INTERNAL_PARTIAL
    DataFlag.FIRST_LOOK -> DataFlagDto.FIRST_LOOK
}

fun LakeState.toDto() = when (this) {
    LakeState.LEARNING -> LakeStateDto.LEARNING
    LakeState.NO_DATA -> LakeStateDto.NO_DATA
    LakeState.STILL -> LakeStateDto.STILL
    LakeState.RIPPLED -> LakeStateDto.RIPPLED
    LakeState.CHOPPY -> LakeStateDto.CHOPPY
}

fun GoalAnswerDto.toModel() = when (this) {
    GoalAnswerDto.YES -> GoalAnswer.YES
    GoalAnswerDto.PARTLY -> GoalAnswer.PARTLY
    GoalAnswerDto.NOT_YET -> GoalAnswer.NOT_YET
}

fun UserClassDto.toModel() = when (this) {
    UserClassDto.IN_SET -> UserClass.IN_SET
    UserClassDto.DEPENDS -> UserClass.DEPENDS
}

// ---- requests in ----
fun WorkSetEntryDto.toModel() = WorkSetEntry(Pkg(pkg), userClass.toModel())
fun StudyBlockDto.toModel() = StudyBlock(startMinute.toInt(), endMinute.toInt())
fun StudyHoursDto.toModel() = StudyHours(blocks.map { it.toModel() }, learnForMe)

/** An unknown id is a bad request; the caller turns null into the BAD_REQUEST error. */
fun suggestionKindOf(kindId: String): SuggestionKind? = SuggestionKind.entries.firstOrNull { it.name == kindId }

// ---- results out ----
fun SaveResult.toDto() = SaveResultDto(ok = ok, savedCount = savedCount.toLong(), userMessage = userMessage)

fun StudyBlockView.toDto() = StudyBlockDto(startMinute.toLong(), endMinute.toLong())
fun PartLines.toDto() = PartLinesDto(stretch = stretch, stays = stays, ret = ret, quiet = quiet)
fun PartsView.toDto() = PartsDto(
    stretchMin = stretchMin, longestStretchMin = longestStretchMin, inSetShare = inSetShare, quietShare = quietShare,
    staysPerHour = staysPerHour, glances = glances.toLong(), returnMin = returnMin, lines = lines.toDto(), extrasLines = extrasLines)
fun SteadinessView.toDto() = SteadinessDto(value = value.toLong(), word = word)
fun StonesView.toDto() = StonesDto(
    totalStays = totalStays.toLong(), stoneCount = stoneCount.toLong(), selfStartedCount = selfStartedCount.toLong(),
    unknownCount = unknownCount.toLong(), topStoneLabel = topStoneLabel, noRippleRate = noRippleRate, line = line)
fun ClearHourView.toDto() = ClearHourDto(startHour.toLong(), endHour.toLong(), stretchMin, line)
fun PatternLine.toDto() = PatternLineDto(kindId, line, evidenceWindows.toLong(), evidenceDays.toLong())
fun SuggestionView.toDto() = SuggestionDto(
    kindId = kindId, line = line, actionLabel = actionLabel, actionType = actionType, opensSettings = opensSettings, subjectKey = subjectKey)
fun ObservationView.toDto() = ObservationDto(kindId, line)
fun VerdictView.toDto() = VerdictDto(verdict = verdict, line = line, approxMix = approxMix, beforeValue = beforeValue, afterValue = afterValue)
fun GoalTapView.toDto() = GoalTapDto(offered = offered, answer = answer?.let { GoalAnswerDto.valueOf(it) })
fun TeacherView.toDto() = TeacherDto(
    opensThisWeek = opensThisWeek.toLong(), minutesThisWeek = minutesThisWeek, line = line, opensPrevWeek = opensPrevWeek?.toLong())
fun SayingView.toDto() = SayingDto(id = id, text = text, source = source, tierLabel = tierLabel, question = question.toLong())

fun MirrorView.toDto() = MirrorDto(
    isDemo = isDemo, provisional = provisional, gentle = gentle, weekStartEpochMs = weekStart.value, weekLabel = weekLabel,
    dataState = dataState.toDto(), dataFlags = dataFlags.map { it.toDto() }, dataLines = dataLines, headline = headline,
    patterns = patterns.map { it.toDto() }, nothingToFix = nothingToFix, goalTap = goalTap.toDto(), reanchorOffered = reanchorOffered,
    parts = parts?.toDto(), steadiness = steadiness?.toDto(), stones = stones?.toDto(), clearHour = clearHour?.toDto(),
    suggestion = suggestion?.toDto(), observation = observation?.toDto(), verdict = verdict?.toDto(), teacher = teacher?.toDto(),
    lapseLine = lapseLine, saying = saying?.toDto(), returnLine = returnLine, suggestedStudyBlock = suggestedStudyBlock?.toDto())

fun WeekRef.toDto() = WeekRefDto(weekStartEpochMs = weekStart.value, label = label, completed = completed)

fun TodayWindowView.toDto() = TodayWindowDto(
    startEpochMs = start.value, endEpochMs = end.value, stays = stays.toLong(), shape = shape, stretchMin = stretchMin, returnMin = returnMin)
fun TodayView.toDto() = TodayDto(
    isDemo = isDemo, windows = windows.map { it.toDto() }, line = line, dataFlags = dataFlags.map { it.toDto() },
    dataLines = dataLines, parts = parts?.toDto())

/** `isDemo` is a host fact (a demo is running), not an engine one. */
fun LakeView.toDto(isDemo: Boolean) = LakeDto(state = state.toDto(), phrase = phrase, isDemo = isDemo, asOfEpochMs = asOf?.value)

fun WhatISeeView.toDto() = WhatISeeDto(
    isDemo = isDemo, usageAccessGranted = usageAccessGranted, notificationAccessGranted = notificationAccessGranted,
    rawEventCount = rawEventCount.toLong(), notifEventCount = notifEventCount.toLong(), derivedDays = derivedDays.toLong(),
    workerRuns7d = workerRuns7d.toLong(), paused = paused, oddEventPairs = oddEventPairs.toLong(), lines = lines,
    oldestRawEpochMs = oldestRawEvent?.value, listenerCoverage7d = listenerCoverage7d, lastWorkerRunEpochMs = lastWorkerRun?.value,
    lastError = lastError)

/** The engine returns the shelf entry; the tier label is fixed text from its letter, and `Q086` becomes 86. */
fun Saying.toDto() = SayingDto(id = id, text = text, source = source, tierLabel = tierLabel(tier), question = q.removePrefix("Q").toLongOrNull() ?: 0L)

// [REFACTOR CANDIDATE: DOC 3 (F13) puts tierLabel in the engine. The façade returns plain Saying, so until T2.15 supplies it
//  the fixed mapping lives here. No tier D saying can be labelled "his own writing".]
internal fun tierLabel(tier: Char): String = when (tier) {
    'A' -> "his own writing or letter"
    'B' -> "recorded lecture"
    'C' -> "reported by others"
    else -> "type not resolved in the Outcome Map"
}

// ---- setup state ----
/** All inputs are plain facts already read from the stores and Android; nothing is decided here. */
fun setupStateDto(
    usageAccessGranted: Boolean, notificationAccessGranted: Boolean, restrictedSettingsSuspected: Boolean,
    settings: Settings, workSetSaved: Boolean, ingest: IngestState, listenerCoverage7d: Double?, isDemo: Boolean) = SetupStateDto(
    usageAccessGranted = usageAccessGranted, notificationAccessGranted = notificationAccessGranted,
    restrictedSettingsSuspected = restrictedSettingsSuspected, workSetSaved = workSetSaved,
    studyHoursSaved = settings.studyBlocks.isNotEmpty() || settings.learnStudyHours,
    batteryHelperShown = settings.batteryHelperShown, weeklyNoteEnabled = settings.weeklyNoteEnabled,
    gentleMode = settings.gentleMode, isDemo = isDemo,
    health = CollectionHealthDto(
        workerRuns7d = ingest.workerRuns7d.toLong(), paused = ingest.paused, lastWorkerRunEpochMs = ingest.lastWorkerRunAt?.value,
        listenerCoverage7d = listenerCoverage7d, lastError = ingest.lastError))

/**
 * Epoch milliseconds back to a week start. A week begins Monday 04:00 local, so this is the study-day rule plus a step back to Monday.
 * [REFACTOR CANDIDATE: use StudyDay.of from Track 2 (T2.1) after the merge so the 04:00 rule lives in one place.]
 */
fun weekStartOf(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): WeekStart {
    val studyDate = Instant.ofEpochMilli(epochMs).atZone(zone).minusHours(Tuning.STUDY_DAY_START_HOUR.toLong()).toLocalDate()
    val monday = studyDate.minusDays((studyDate.dayOfWeek.value - 1).toLong())
    return WeekStart(StudyDay(monday.toEpochDay()))
}
