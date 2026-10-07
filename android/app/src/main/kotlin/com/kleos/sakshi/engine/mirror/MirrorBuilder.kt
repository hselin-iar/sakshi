package com.kleos.sakshi.engine.mirror

import com.kleos.sakshi.engine.metrics.Parts
import com.kleos.sakshi.engine.metrics.Weeks
import com.kleos.sakshi.engine.metrics.steadiness
import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.ClearHourView
import com.kleos.sakshi.engine.model.DataFlag
import com.kleos.sakshi.engine.model.DataGap
import com.kleos.sakshi.engine.model.DataState
import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.GapKind
import com.kleos.sakshi.engine.model.GoalTapView
import com.kleos.sakshi.engine.model.LapseInfo
import com.kleos.sakshi.engine.model.MirrorView
import com.kleos.sakshi.engine.model.NotifKind
import com.kleos.sakshi.engine.model.ObservationView
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.PartLines
import com.kleos.sakshi.engine.model.PartsView
import com.kleos.sakshi.engine.model.PatternLine
import com.kleos.sakshi.engine.model.SayingView
import com.kleos.sakshi.engine.model.SteadinessView
import com.kleos.sakshi.engine.model.StonesView
import com.kleos.sakshi.engine.model.StudyBlockView
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.SuggestionView
import com.kleos.sakshi.engine.model.TeacherView
import com.kleos.sakshi.engine.model.Verdict
import com.kleos.sakshi.engine.model.VerdictView
import com.kleos.sakshi.engine.model.WeekStart
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.patterns.PatternContext
import com.kleos.sakshi.engine.patterns.PatternEngine
import com.kleos.sakshi.engine.patterns.Rhythm
import com.kleos.sakshi.engine.ports.Ports
import com.kleos.sakshi.engine.suggestions.SelectionResult
import com.kleos.sakshi.engine.suggestions.SuggestionContext
import com.kleos.sakshi.engine.suggestions.SuggestionRegistry
import com.kleos.sakshi.engine.suggestions.SuggestionSelector
import com.kleos.sakshi.engine.tuning.Tuning
import com.kleos.sakshi.engine.usecases.BuildWeekSummary
import com.kleos.sakshi.engine.usecases.SayingTags
import com.kleos.sakshi.engine.usecases.UpdateBaseline
import com.kleos.sakshi.engine.usecases.effectiveGentle
import com.kleos.sakshi.engine.windows.StudyHoursLearner
import com.kleos.sakshi.engine.classify.AppClassifier
import com.kleos.sakshi.engine.stays.StoneWave
import java.time.ZoneId

/** What the façade needs besides the view: the things a viewed Mirror changes (shown suggestion, shown verdict, acknowledged lapse). */
data class MirrorFacts(
    val week: WeekStart,
    val completed: Boolean,
    val weekNumber: Int,
    val currentTask: Candidate?,
    val lapse: LapseInfo?,
    val lapseShown: Boolean,
    val verdictExperimentId: Long?,
    val sayingTags: Set<String>,
)

data class BuiltMirror(val view: MirrorView, val facts: MirrorFacts)

/**
 * F6: the one assembly point of the Mirror, in DOC 3's compute order. It only reads. A section that cannot be computed is null with
 * its honest data-state sentence, never a zero. Gentle mode is applied last so no feature can bypass it.
 */
object MirrorBuilder {
    private const val DAY_MS = 86_400_000L

    fun build(ports: Ports, requested: WeekStart?, asOf: EpochMs, zone: ZoneId, isDemo: Boolean): BuiltMirror {
        val settings = ports.state.settings()
        val baseline = ports.state.baseline()?.takeIf { it.isActive }
        val today = StudyDay.of(asOf, zone)
        val allDays = ports.derived.days(StudyDay(0), today)
        val gaps = ports.gaps.overlapping(EpochMs(0), EpochMs(asOf.value + 1))
        val firstReadWeek = settings.firstReadAt?.let { Weeks.of(it, zone) }
        val firstReadDay = settings.firstReadAt?.let { StudyDay.of(it, zone) }

        // 1. which week: the one asked for, else the latest completed week with enough valid days, else the First Look
        val latest = BuildWeekSummary.latestMirrorWeek(ports, asOf, zone)
        val firstLook = requested == null && latest == null
        val week = requested ?: latest ?: Weeks.of(asOf, zone)
        val completed = !firstLook && Weeks.isCompleted(week, asOf, zone)
        val provisional = firstLook || baseline == null || !completed
        val weekNumber = firstReadWeek?.let { Weeks.between(it, week) + 1 }?.coerceAtLeast(1) ?: 1

        val scopeFrom = if (firstLook) EpochMs(0) else Weeks.startMs(week, zone)
        val scopeTo = if (firstLook) EpochMs(asOf.value + 1) else Weeks.endMs(week, zone)
        val scopeDays = if (firstLook) allDays else allDays.filter { Weeks.startOf(it.day) == week }
        val validScope = scopeDays.filter { it.valid }.map { it.day }.toSet()
        val inScope = ports.derived.windows(scopeFrom, scopeTo)
        // First Look shows whatever finished, valid day or not; a Mirror week pools valid days only (F5)
        val scopeWindows = if (firstLook) inScope.filter { !it.window.partial && it.window.finalised }
        else BuildWeekSummary.qualifying(inScope, validScope)

        // 2. parts, and Steadiness only against an active baseline, from a completed week, with all four parts
        val parts = BuildWeekSummary.pool(scopeWindows)
        val steady = if (!provisional && baseline != null) steadiness(parts, baseline) else null

        // honest data states (F17)
        val everHeard = ports.coverage.sessions(EpochMs(0), EpochMs(asOf.value + 1)).isNotEmpty()
        val meanWeekly = BuildWeekSummary.baselineMeanWeeklyMinutes(ports, baseline, zone)
        val coverageFrom = if (firstLook) EpochMs(scopeDays.minOfOrNull { it.day.startEpochMs(zone).value } ?: (asOf.value - 3 * DAY_MS)) else scopeFrom
        val notSeenDays = daysTouchedBy(scopeDays, gaps, GapKind.NOT_SEEN, zone)
        val pausedDays = daysTouchedBy(scopeDays, gaps, GapKind.PAUSED, zone)
        val flags = DataStates.flags(
            notificationAccessGranted = true, everHadListenerSession = everHeard,
            listenerCoverageFraction = ports.coverage.coverageFraction(coverageFrom, EpochMs(minOf(scopeTo.value, asOf.value))),
            notSeenDays = notSeenDays, pausedDays = pausedDays, nonPartialWindowCount = scopeWindows.size, validDayCount = validScope.size,
            weekWindowMinutes = scopeWindows.sumOf { (it.window.end.value - it.window.start.value) / 60_000.0 },
            baselineMeanWeeklyMinutes = meanWeekly, provisional = provisional,
        )
        val dataLines = (SentenceBuilder.dataLines(flags, notSeenDays, pausedDays, lastPingDaysAgo(ports, asOf)) +
            learningLines(baseline != null, allDays, firstReadDay)).toMutableList()
        val dataState = when {
            DataFlag.TOO_LITTLE_DATA in flags -> DataState.TOO_LITTLE_DATA
            baseline == null -> DataState.LEARNING_BASELINE
            else -> DataState.OK
        }

        // 3. stones, the clear hour and the pattern lines
        val stays = scopeWindows.flatMap { it.stays }
        val stones = stonesView(ports, stays, scopeWindows)
        val patternCtx = PatternContext(
            windows = ports.derived.windows(EpochMs(0), asOf), weeks = ports.derived.weeks(), days = allDays, baseline = baseline,
            asOf = asOf, random = ports.random, zone = zone,
        )
        val clearHour = if (provisional) null else Rhythm.clearHour(patternCtx)
        val livePatterns = ports.derived.patterns().filterNot { PatternEngine.isRetired(it, asOf) }
        val patternLines = if (provisional) emptyList()
        else livePatterns.sortedByDescending { it.evidenceWindows }.take(MAX_PATTERN_LINES)
            .map { PatternLine(it.kind.name, SentenceBuilder.patternLine(it), it.evidenceWindows, it.evidenceDays) }

        // the lapse line is said once (F15)
        val lapse = firstReadDay?.let { Lapse.detect(allDays, gaps, it, zone) }?.takeIf { it.endedAt.epochDay < today.epochDay }
        val ack = settings.lapseAcknowledgedThrough
        val lapseUnacknowledged = lapse != null && (ack == null || ack.epochDay < lapse.endedAt.epochDay)

        // 4. one task or "nothing to fix"; the learning sentence instead when there is not enough data
        val selection: SelectionResult?
        val candidates: List<Candidate>
        if (provisional) {
            selection = null; candidates = emptyList()
        } else {
            val selAsOf = EpochMs(minOf(asOf.value, scopeTo.value))
            val ctx = suggestionContext(
                ports, week, weekNumber, scopeDays, allDays, baseline, livePatterns, clearHour, lapse.takeIf { lapseUnacknowledged },
                lapseUnacknowledged, everHeard, selAsOf, zone,
            )
            candidates = SuggestionRegistry.all.mapNotNull { rule -> runCatching { rule.evaluate(ctx) }.getOrNull() }   // one broken rule never silences the rest
            selection = SuggestionSelector.select(ctx, candidates)
        }
        val task = selection?.task
        val nothingToFix = selection != null && task == null && selection.reason != com.kleos.sakshi.engine.suggestions.SilenceReason.NOT_ENOUGH_DATA
        if (selection?.reason == com.kleos.sakshi.engine.suggestions.SilenceReason.NOT_ENOUGH_DATA) dataLines += SentenceBuilder.notEnoughForSuggestion()

        // 5. the verdict that has landed and not been shown, and the one-tap goal question
        val verdictExp = ports.state.experiments().filter { it.verdict != Verdict.PENDING && !it.shown }.maxByOrNull { it.startedAt.value }
        val verdict = verdictExp?.let { e ->
            SentenceBuilder.verdictLine(e.verdict, e.target, e.beforeValue, e.afterValue, e.approxMix)?.let {
                VerdictView(e.verdict.name, it, e.beforeValue, e.afterValue, e.approxMix)
            }
        }
        val weeksSinceBaseline = baseline?.let { Weeks.between(Weeks.of(it.frozenAt, zone), week) + 1 }
        val answered = ports.state.goalTaps().firstOrNull { it.weekStart == week }
        val goalTap = when {
            answered != null -> GoalTapView(true, answered.answer.name)
            baseline != null && weeksSinceBaseline != null && weeksSinceBaseline <= Tuning.GOAL_TAP_LAST_WEEK && !provisional -> GoalTapView(true, null)
            else -> GoalTapView(false, null)
        }

        // 6. the teacher line (from the second week), the lapse line, the saying, the data flags
        val own = BuildWeekSummary.ownUse(ports, week, asOf, zone)
        val prevOwn = if (weekNumber >= 2) BuildWeekSummary.ownUse(ports, Weeks.previous(week), asOf, zone) else null
        val teacher = if (!firstLook && weekNumber >= 2 && own.opens > 0)
            TeacherView(own.opens, prevOwn?.opens, own.minutes, SentenceBuilder.teacherLine(own.opens, prevOwn?.opens, weekNumber - 1)) else null
        val lapseLine = if (lapseUnacknowledged) lapse?.let { SentenceBuilder.lapseLine(it.days) } else null
        val saying = ports.state.sayingPicks().maxByOrNull { it.pickedAt.value }?.let { pick ->
            ports.shelf.all().firstOrNull { it.id == pick.sayingId }?.let {
                SayingView(it.id, it.text, it.source, SentenceBuilder.tierLabel(it.tier), it.q.removePrefix("Q").toIntOrNull() ?: 0)
            }
        }
        val returnLine = parts.returnMin?.let { SentenceBuilder.gentleReturnLine(it) }

        // 7. re-anchor, only once and only from week 4; the study-block suggestion is not built (T2.4: learn-study-hours is optional)
        val reanchorOffered = !provisional && baseline != null && baseline.id == UpdateBaseline.FIRST_BASELINE_ID &&
            weeksSinceBaseline != null && weeksSinceBaseline >= Tuning.REANCHOR_MIN_WEEK

        // learned study hours are offered, never applied (F3); from week 3, when learning is on and the block differs by an hour or more
        val suggestedBlock = if (provisional || !settings.learnStudyHours || weekNumber < 3) null
        else {
            val validEver = allDays.filter { it.valid }.map { it.day }.toSet()
            val history = ports.derived.windows(EpochMs(0), asOf).filter { it.window.day in validEver }.map { it.window }
            StudyHoursLearner.suggest(StudyHoursLearner.learn(history, zone), settings.studyBlocks)?.let { StudyBlockView(it.startMinute, it.endMinute) }
        }

        val headline = when {
            parts.stretchMin == null -> if (baseline == null) SentenceBuilder.learningHeadline() else SentenceBuilder.tooLittleHeadline()
            provisional -> parts.staysPerHour?.let { SentenceBuilder.provisionalHeadline(parts.stretchMin, it) } ?: SentenceBuilder.learningHeadline()
            steady != null && parts.returnMin != null -> SentenceBuilder.headline(parts.stretchMin, parts.returnMin, steady.word)
            else -> SentenceBuilder.tooLittleHeadline()
        }

        val view = MirrorView(
            isDemo = isDemo, provisional = provisional, gentle = false,
            weekStart = Weeks.startMs(week, zone), weekLabel = if (firstLook) SentenceBuilder.firstLookLabel() else SentenceBuilder.weekLabel(week, zone),
            dataState = dataState, dataFlags = flags, dataLines = dataLines, headline = headline,
            parts = if (parts.stretchMin == null) null else partsView(parts, scopeWindows.sumOf { it.glances }),
            steadiness = steady?.let { SteadinessView(it.value, SentenceBuilder.steadinessWord(it.word)) },
            stones = stones, clearHour = clearHour?.let { ClearHourView(it.startHour, it.endHour, it.stretchMin, SentenceBuilder.clearHourLine(it.startHour, it.endHour, it.stretchMin)) },
            patterns = patternLines,
            suggestion = task?.let { SuggestionView(it.kind.name, it.subject?.value, SentenceBuilder.suggestionLine(it), SentenceBuilder.actionLabel(it.action), it.action.name, it.action == com.kleos.sakshi.engine.model.ActionType.OPEN_NOTIFICATION_SETTINGS) },
            observation = selection?.observation?.let { ObservationView(it.kind.name, SentenceBuilder.observationLine(it)) },
            nothingToFix = nothingToFix, verdict = verdict, goalTap = goalTap, teacher = teacher, lapseLine = lapseLine, saying = saying,
            returnLine = returnLine, reanchorOffered = reanchorOffered, suggestedStudyBlock = suggestedBlock,
        )

        val knownStays = stays.filter { it.origin == Origin.STONE || it.origin == Origin.SELF_STARTED }
        val tags = buildSet {
            if (knownStays.isNotEmpty() && knownStays.count { it.origin == Origin.STONE }.toDouble() / knownStays.size >= 0.5) add(SayingTags.STONE_AND_WAVE)
            if (baseline != null && parts.returnMin != null && parts.returnMin > baseline.r0) add(SayingTags.RETURN)
            if (baseline != null && parts.quietShare != null && parts.quietShare > baseline.q0) add(SayingTags.QUIET_RHYTHM)
            if (candidates.any { it.kind == SuggestionKind.S10 }) add(SayingTags.GLANCE_REASSURANCE)
            if (weekNumber >= 8) add(SayingTags.TEACHER_LEAVES)
            if (candidates.any { it.kind == SuggestionKind.S12 }) add(SayingTags.WINS)
            if (lapseLine != null) add(SayingTags.AFTER_LAPSE)
        }

        val gentle = settings.effectiveGentle()
        return BuiltMirror(
            view = if (gentle) GentlePolicy.apply(view) else view,
            facts = MirrorFacts(week, completed, weekNumber, task, lapse, lapseLine != null, verdictExp?.id.takeIf { verdict != null }, tags),
        )
    }

    private const val MAX_PATTERN_LINES = 4

    fun partsView(parts: Parts, glances: Int) = PartsView(
        stretchMin = parts.stretchMin, longestStretchMin = parts.longestStretchMin, inSetShare = parts.inSetShare, quietShare = parts.quietShare,
        staysPerHour = parts.staysPerHour, glances = glances, returnMin = parts.returnMin,
        lines = PartLines(
            stretch = parts.stretchMin?.let { SentenceBuilder.stretchLine(it, parts.longestStretchMin) },
            stays = parts.staysPerHour?.let { SentenceBuilder.staysLine(it) },
            ret = parts.returnMin?.let { SentenceBuilder.returnLine(it) },
            quiet = parts.quietShare?.let { SentenceBuilder.quietLine(it) },
        ),
        extrasLines = emptyList(),
    )

    private fun stonesView(ports: Ports, stays: List<com.kleos.sakshi.engine.model.Stay>, windows: List<WindowWithDetail>): StonesView? {
        if (stays.isEmpty()) return null
        val stone = stays.count { it.origin == Origin.STONE }
        val self = stays.count { it.origin == Origin.SELF_STARTED }
        val topPkg = stays.filter { it.origin == Origin.STONE }.groupBy { it.stonePkg }.maxByOrNull { it.value.size }?.key
        val label = topPkg?.let { p -> ports.catalog.launcherApps().firstOrNull { it.pkg == p }?.label }
        return StonesView(stays.size, stone, self, stays.count { it.origin == Origin.UNKNOWN }, label, noRippleRate(ports, windows), SentenceBuilder.stonesLine(stays.size, stone, self))
    }

    /**
     * Of the pings that arrived during a window, the share that led to no stay within a minute: the "waves that never came" (F4). Pooled
     * over every window; null when no covered ping qualified, never zero.
     */
    private fun noRippleRate(ports: Ports, windows: List<WindowWithDetail>): Double? {
        val classes = ports.state.apps().associate { it.pkg to it.userClass }
        val classifier = AppClassifier(classes, ports.catalog)
        var pings = 0.0; var without = 0.0
        for (w in windows) {
            val notifs = ports.notifs.range(w.window.start, w.window.end)
            val rate = StoneWave.noRippleRate(w.window, notifs, ports.coverage, w.stays) { classifier.classify(it) } ?: continue
            val n = notifs.count { it.kind == NotifKind.POSTED && !it.ongoing && classifier.classify(it.pkg) != com.kleos.sakshi.engine.model.AppClass.NEUTRAL }
            pings += n; without += rate * n
        }
        return if (pings > 0) without / pings else null
    }

    /** Study days of the scope that any gap of this kind touches: the sentence names days, never windows. */
    private fun daysTouchedBy(days: List<DaySummary>, gaps: List<DataGap>, kind: GapKind, zone: ZoneId): Int = days.count { d ->
        val from = d.day.startEpochMs(zone).value
        val to = d.day.endEpochMs(zone).value
        gaps.any { it.kind == kind && it.start.value < to && (it.end?.value ?: Long.MAX_VALUE) > from }
    }

    private fun lastPingDaysAgo(ports: Ports, asOf: EpochMs): Int? {
        val last = ports.notifs.range(EpochMs(asOf.value - 30 * DAY_MS), EpochMs(asOf.value + 1)).filter { it.kind == NotifKind.POSTED }.maxByOrNull { it.ts.value } ?: return null
        return ((asOf.value - last.ts.value) / DAY_MS).toInt()
    }

    private fun learningLines(hasBaseline: Boolean, allDays: List<DaySummary>, firstReadDay: StudyDay?): List<String> {
        if (hasBaseline) return emptyList()
        val seen = if (firstReadDay == null) 0 else allDays.count { it.valid && it.day.epochDay >= firstReadDay.epochDay }
        return listOf(SentenceBuilder.learningLine(minOf(seen, Tuning.BASELINE_VALID_DAYS), Tuning.BASELINE_VALID_DAYS))
    }

    private fun suggestionContext(
        ports: Ports, week: WeekStart, weekNumber: Int, weekDays: List<DaySummary>, allDays: List<DaySummary>,
        baseline: com.kleos.sakshi.engine.model.Baseline?, patterns: List<com.kleos.sakshi.engine.model.Pattern>,
        clearHour: com.kleos.sakshi.engine.patterns.ClearHourResult?, lapse: LapseInfo?, firstAfterLapse: Boolean,
        everHeard: Boolean, selAsOf: EpochMs, zone: ZoneId,
    ): SuggestionContext {
        val valid = allDays.filter { it.valid }.map { it.day }.toSet()
        val windows = BuildWeekSummary.qualifying(ports.derived.windows(EpochMs(selAsOf.value - 14 * DAY_MS), selAsOf), valid)
        val weekWindows = BuildWeekSummary.qualifying(ports.derived.windows(Weeks.startMs(week, zone), Weeks.endMs(week, zone)), valid)
        val summary = BuildWeekSummary.summarize(
            week, weekDays, weekWindows, baseline, BuildWeekSummary.baselineMeanWeeklyMinutes(ports, baseline, zone),
            BuildWeekSummary.ownUse(ports, week, selAsOf, zone), 0,
        )
        return SuggestionContext(
            windows = windows, week = summary, priorWeeks = ports.derived.weeks().filter { it.weekStart.studyDay.epochDay < week.studyDay.epochDay }.sortedBy { it.weekStart.studyDay.epochDay },
            days = allDays, baseline = baseline, patterns = patterns, clearHour = clearHour, lapse = lapse, isFirstMirrorAfterLapse = firstAfterLapse,
            currentWeekNumber = weekNumber, notificationAccessGranted = everHeard,
            listenerCoverageFraction14d = ports.coverage.coverageFraction(EpochMs(selAsOf.value - 14 * DAY_MS), selAsOf),
            // a suggestion shown in THIS week is the one on screen now, not a reason to be quiet about it on the next view of the same week
            suggestionStates = ports.state.suggestionStates().map { if (it.shownInWeek == week) it.copy(shownInWeek = null) else it }, experiments = ports.state.experiments(), asOf = selAsOf, zone = zone,
        )
    }
}
