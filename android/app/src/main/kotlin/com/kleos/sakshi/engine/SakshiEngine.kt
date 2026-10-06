package com.kleos.sakshi.engine

import com.kleos.sakshi.engine.model.*
import com.kleos.sakshi.engine.ports.Ports

/**
 * The façade (LC-3). T1.2 stubs: every method returns an empty-but-valid view with `provisional = true`, or is a no-op.
 * No engine logic here; Track 2 replaces each body.
 */
class SakshiEngine(@Suppress("unused") private val ports: Ports) {
    fun processNewEvents(asOf: EpochMs): ProcessReport = ProcessReport(lakeChanged = false)

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
