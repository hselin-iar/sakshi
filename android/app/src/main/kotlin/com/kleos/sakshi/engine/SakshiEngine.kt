package com.kleos.sakshi.engine

import com.kleos.sakshi.engine.lake.LakeBuilder
import com.kleos.sakshi.engine.metrics.Weeks
import com.kleos.sakshi.engine.mirror.MirrorBuilder
import com.kleos.sakshi.engine.mirror.SentenceBuilder
import com.kleos.sakshi.engine.mirror.TodayBuilder
import com.kleos.sakshi.engine.model.*
import com.kleos.sakshi.engine.note.NoteContext
import com.kleos.sakshi.engine.note.WeeklyNote
import com.kleos.sakshi.engine.ports.Ports
import com.kleos.sakshi.engine.privacy.ExportBuilder
import com.kleos.sakshi.engine.privacy.WhatISeeBuilder
import com.kleos.sakshi.engine.usecases.BuildWeekSummary
import com.kleos.sakshi.engine.usecases.ChooseSayings
import com.kleos.sakshi.engine.usecases.DeleteEverything
import com.kleos.sakshi.engine.usecases.DismissSuggestion
import com.kleos.sakshi.engine.usecases.FinalizeWindows
import com.kleos.sakshi.engine.usecases.FootprintStart
import com.kleos.sakshi.engine.usecases.GoalTapUseCase
import com.kleos.sakshi.engine.usecases.JudgeExperiments
import com.kleos.sakshi.engine.usecases.PauseCollection
import com.kleos.sakshi.engine.usecases.PickSaying
import com.kleos.sakshi.engine.usecases.Reanchor
import com.kleos.sakshi.engine.usecases.RecomputeDay
import com.kleos.sakshi.engine.usecases.RunPatterns
import com.kleos.sakshi.engine.usecases.SettingsUseCases
import com.kleos.sakshi.engine.usecases.TapOutcome
import com.kleos.sakshi.engine.usecases.TapTryThis
import com.kleos.sakshi.engine.usecases.UpdateBaseline
import java.time.ZoneId

/**
 * The façade (LC-3). It only sequences use cases and builders; every decision about attention lives in them.
 * `isDemo` is true for the Time Machine's own store, so a demo view says so. It changes nothing else: the demo exercises the same
 * use cases as a real phone, over a throwaway store (DOC 3, Time Machine Demo).
 */
class SakshiEngine(private val ports: Ports, private val isDemo: Boolean = false) {
    // No Settings field carries the user's zone and the locked signatures cannot take one, so the device's configured zone is read.
    // That is neither wall-clock time nor randomness; it is a gap worth a Settings field later.
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun processNewEvents(asOf: EpochMs): ProcessReport {
        SettingsUseCases.ensureInitialised(ports, asOf)
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
        val lakeChanged = refreshDerived(asOf)
        return ProcessReport(
            daysRecomputed = touchedDays.size + staleDays.size, newWindows = newWindows,
            baselineFrozen = baselineFrozen, lakeChanged = lakeChanged,
        )
    }

    /** Everything that is built from the derived days: weeks, the Mirror-ready mark, patterns, experiments, the Lake row. True if the Lake changed. */
    private fun refreshDerived(asOf: EpochMs): Boolean {
        BuildWeekSummary.run(ports, asOf, zone)
        val ready = BuildWeekSummary.latestMirrorWeek(ports, asOf, zone)
        if (ready != null) ports.state.saveNote(ports.state.note().copy(mirrorReadyWeek = ready))
        RunPatterns.run(ports, asOf, zone)
        FootprintStart.run(ports, asOf, zone)
        JudgeExperiments.run(ports, asOf, zone)

        val before = ports.state.lake()
        val view = LakeBuilder.build(ports, asOf, zone)
        val row = LakeRow(view.state, view.phrase, view.asOf)
        ports.state.saveLake(row)
        return before != row
    }

    /** A changed work set or study block changes which windows exist: derived rows are thrown away and rebuilt from the raw events. The frozen baseline stays. */
    private fun rederive() {
        ports.derived.clearDerived()
        processNewEvents(ports.clock.now())
    }

    fun mirror(week: WeekStart?, asOf: EpochMs): MirrorView {
        val built = MirrorBuilder.build(ports, week, asOf, zone, isDemo)
        val f = built.facts
        if (!f.completed) return built.view
        // Seeing a Mirror is what changes state: the week counts as read, a verdict is marked shown, a suggestion is marked offered, a lapse is acknowledged.
        val note = ports.state.note()
        if (note.mirrorViewedWeek != f.week) ports.state.saveNote(note.copy(mirrorViewedWeek = f.week))
        f.verdictExperimentId?.let { id -> ports.state.experiments().firstOrNull { it.id == id }?.let { ports.state.saveExperiment(it.copy(shown = true)) } }
        f.currentTask?.takeIf { built.view.suggestion != null }?.let { t ->
            val existing = ports.state.suggestionStates().firstOrNull { it.kind == t.kind && it.subject == t.subject }
            ports.state.saveSuggestionState(
                (existing ?: SuggestionState(t.kind, t.subject, asOf, null, null, null, null, "shown")).copy(lastShownAt = asOf, shownInWeek = f.week),
            )
        }
        if (f.lapseShown) f.lapse?.let { l ->
            ports.state.saveSettings(ports.state.settings().copy(lapseAcknowledgedThrough = l.endedAt))
        }
        return built.view
    }

    fun listMirrorWeeks(asOf: EpochMs): List<WeekRef> {
        val weeks = ports.derived.weeks().filter { it.validDays >= BuildWeekSummary.MIRROR_MIN_VALID_DAYS }
            .map { it.weekStart }.filter { Weeks.isCompleted(it, asOf, zone) }.sortedByDescending { it.studyDay.epochDay }
        return weeks.map { WeekRef(Weeks.startMs(it, zone), SentenceBuilder.weekLabel(it, zone), completed = true) }
    }

    fun today(asOf: EpochMs): TodayView = TodayBuilder.build(ports, asOf, zone, isDemo)

    fun lake(asOf: EpochMs): LakeView = LakeBuilder.build(ports, asOf, zone)

    fun whatISee(asOf: EpochMs, host: HostFacts): WhatISeeView = WhatISeeBuilder.build(ports, asOf, host, zone, isDemo)

    fun saveWorkSet(entries: List<WorkSetEntry>): SaveResult {
        val result = SettingsUseCases.saveWorkSet(ports, entries, ports.clock.now())
        if (result.ok) rederive()
        return result
    }

    fun saveStudyHours(h: StudyHours) {
        SettingsUseCases.saveStudyHours(ports, h)
        rederive()
    }

    fun setGentle(on: Boolean) = SettingsUseCases.setGentle(ports, on)
    fun setUnder18(on: Boolean) = SettingsUseCases.setUnder18(ports, on)
    fun setWeeklyNote(on: Boolean) = SettingsUseCases.setWeeklyNote(ports, on)
    fun markBatteryHelperShown() = SettingsUseCases.markBatteryHelperShown(ports)

    fun tapTryThis(kind: SuggestionKind, subject: Pkg?, asOf: EpochMs): TapResult {
        val current = MirrorBuilder.build(ports, null, asOf, zone, isDemo).facts.currentTask
        return when (TapTryThis.run(ports, kind, subject, current, asOf)) {
            TapOutcome.STARTED, TapOutcome.ALREADY_RUNNING -> TapResult(ok = true, reason = null)
            TapOutcome.STALE -> TapResult(ok = false, reason = "STALE")
        }
    }

    fun dismissSuggestion(kind: SuggestionKind, subject: Pkg?, asOf: EpochMs) = DismissSuggestion.run(ports, kind, subject, asOf)

    fun tapGoal(answer: GoalAnswer, asOf: EpochMs) {
        val week = BuildWeekSummary.latestMirrorWeek(ports, asOf, zone) ?: return
        GoalTapUseCase.run(ports, answer, week)
    }

    fun reanchor(asOf: EpochMs): ReanchorResult = Reanchor.run(ports, asOf, zone)

    fun chooseSayings(asOf: EpochMs): List<Saying> {
        val facts = MirrorBuilder.build(ports, null, asOf, zone, isDemo).facts
        return ChooseSayings.run(ports, facts.sayingTags, facts.weekNumber, asOf)
    }

    fun pickSaying(id: String, asOf: EpochMs) = PickSaying.run(ports, id, asOf)

    fun pause(on: Boolean, asOf: EpochMs) = PauseCollection.run(ports, on, asOf)

    fun export(includeRaw: Boolean, asOf: EpochMs): ExportDocument = ExportBuilder.build(includeRaw, asOf)

    fun deleteEverything() = DeleteEverything.run(ports)

    fun noteDecision(asOf: EpochMs, host: HostFacts): NoteDecision {
        val settings = ports.state.settings()
        val state = ports.state.note()
        val week = BuildWeekSummary.latestMirrorWeek(ports, asOf, zone)
        val validDays = week?.let { w -> ports.derived.days(w.studyDay, StudyDay(w.studyDay.epochDay + 6)).count { it.valid } } ?: 0
        val live = ports.derived.windows(EpochMs(asOf.value - 24 * 3_600_000L), EpochMs(asOf.value + 1)).any { !it.window.finalised }
        return WeeklyNote.decide(
            NoteContext(
                asOf = asOf, enabled = settings.weeklyNoteEnabled, canPost = host.canPostNotifications, lastCompletedWeek = week,
                validDaysInThatWeek = validDays, lastNoteWeek = state.lastNoteWeek, mirrorViewedWeek = state.mirrorViewedWeek,
                inWindowNow = live, zone = zone,
            ),
        )
    }
}
