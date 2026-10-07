package com.kleos.sakshi.engine

import com.kleos.sakshi.engine.model.*
import com.kleos.sakshi.engine.ports.Ports
import com.kleos.sakshi.engine.usecases.FinalizeWindows
import com.kleos.sakshi.engine.usecases.RecomputeDay
import com.kleos.sakshi.engine.usecases.UpdateBaseline
import java.time.ZoneId

/**
 * The façade (LC-3). T1.2 stubs: every method returns an empty-but-valid view with `provisional = true`, or is a no-op.
 * No engine logic here; Track 2 replaces each body.
 */
class SakshiEngine(private val ports: Ports) {
    // T2.8: no Settings field carries the user's zone, and this locked signature can't take one as a
    // parameter, so this reads the device's configured zone -- not wall-clock time or randomness, so
    // DependencyRuleTest's ban on those doesn't cover it, but it's a gap worth a Settings field later.
    fun processNewEvents(asOf: EpochMs): ProcessReport {
        val zone = ZoneId.systemDefault()
        val oldest = ports.events.oldest() ?: return ProcessReport()

        val touchedDays = ports.events.range(oldest, asOf)
            .map { StudyDay.of(it.ts, zone) }
            .distinct()
            .sortedBy { it.epochDay }

        var newWindows = 0
        for (day in touchedDays) {
            val derivation = RecomputeDay.recompute(day, asOf, ports, zone)
            ports.derived.replaceDay(day, derivation)
            newWindows += derivation.windows.size
        }

        val today = StudyDay.of(asOf, zone)
        val lookback = (0..2).map { StudyDay(today.epochDay - it) }.filterNot { it in touchedDays }
        val staleDays = FinalizeWindows.staleDays(ports, asOf, lookback, zone)
        for (day in staleDays) {
            ports.derived.replaceDay(day, RecomputeDay.recompute(day, asOf, ports, zone))
        }

        val baselineFrozen = UpdateBaseline.run(ports, asOf, zone)

        return ProcessReport(
            daysRecomputed = touchedDays.size + staleDays.size,
            newWindows = newWindows,
            baselineFrozen = baselineFrozen,
        )
    }

    fun mirror(week: WeekStart?, asOf: EpochMs): MirrorView = MirrorView(
        isDemo = false, provisional = true, gentle = false, weekStart = asOf, weekLabel = "",
        dataState = DataState.LEARNING_BASELINE, dataFlags = emptyList(), dataLines = emptyList(), headline = "",
        parts = null, steadiness = null, stones = null, clearHour = null, patterns = emptyList(),
        suggestion = null, observation = null, nothingToFix = false, verdict = null,
        goalTap = GoalTapView(offered = false, answer = null), teacher = null, lapseLine = null, saying = null,
        returnLine = null, reanchorOffered = false, suggestedStudyBlock = null)

    fun listMirrorWeeks(asOf: EpochMs): List<WeekRef> = emptyList()

    fun today(asOf: EpochMs): TodayView = TodayView(
        isDemo = false, windows = emptyList(), parts = null, line = "", dataFlags = emptyList(), dataLines = emptyList())

    fun lake(asOf: EpochMs): LakeView = LakeView(state = LakeState.NO_DATA, phrase = "", asOf = null)

    fun whatISee(asOf: EpochMs, host: HostFacts): WhatISeeView = WhatISeeView(
        isDemo = false, usageAccessGranted = host.usageAccessGranted, notificationAccessGranted = host.notificationAccessGranted,
        rawEventCount = 0, notifEventCount = 0, oldestRawEvent = null, derivedDays = 0, listenerCoverage7d = null,
        lastWorkerRun = null, workerRuns7d = 0, paused = false, lastError = host.lastError, oddEventPairs = 0, lines = emptyList())

    fun saveWorkSet(entries: List<WorkSetEntry>): SaveResult = SaveResult(ok = true, savedCount = 0, userMessage = null)
    fun saveStudyHours(h: StudyHours) {}

    fun setGentle(on: Boolean) {}
    fun setUnder18(on: Boolean) {}
    fun setWeeklyNote(on: Boolean) {}
    fun markBatteryHelperShown() {}

    fun tapTryThis(kind: SuggestionKind, subject: Pkg?, asOf: EpochMs): TapResult = TapResult(ok = false, reason = null)
    fun dismissSuggestion(kind: SuggestionKind, subject: Pkg?, asOf: EpochMs) {}
    fun tapGoal(answer: GoalAnswer, asOf: EpochMs) {}
    fun reanchor(asOf: EpochMs): ReanchorResult = ReanchorResult(ok = false, reason = null)

    fun chooseSayings(asOf: EpochMs): List<Saying> = emptyList()
    fun pickSaying(id: String, asOf: EpochMs) {}

    fun pause(on: Boolean, asOf: EpochMs) {}
    fun export(includeRaw: Boolean, asOf: EpochMs): ExportDocument =
        ExportDocument(exportVersion = 1, exportedAt = asOf, includeRaw = includeRaw)
    fun deleteEverything() {}
    fun noteDecision(asOf: EpochMs, host: HostFacts): NoteDecision = NoteDecision(post = false, reason = null)
}
