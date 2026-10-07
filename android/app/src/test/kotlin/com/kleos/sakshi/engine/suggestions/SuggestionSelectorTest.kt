package com.kleos.sakshi.engine.suggestions

import com.kleos.sakshi.engine.model.ActionType
import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Experiment
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.StartReason
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.SuggestionState
import com.kleos.sakshi.engine.model.TargetMetric
import com.kleos.sakshi.engine.model.Verdict
import com.kleos.sakshi.engine.model.WeekStart
import com.kleos.sakshi.engine.model.WeekSummary
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.model.WindowSource
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.testkit.Zones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** G-G1..G-G7 are the spec (DOC 3/DOC4, F11. The Suggestions Engine: SuggestionSelector). */
class SuggestionSelectorTest {
    private val zone = Zones.KOLKATA
    private val weekStart = WeekStart(StudyDay(70)) // arbitrary Monday-aligned study day

    private fun validDays(n: Int) = (0 until n).map {
        DaySummary(
            StudyDay(it.toLong()), valid = true, windowMinutes = 0.0, quietMinutes = 0.0, inSetMinutes = 0.0,
            coverage = 1.0, pickups = 0, switchesPerHour = null, flinch = null, rampUpMin = null,
            lastScreenOffTs = null, firstStretchMin = null, externalResumes = 0,
        )
    }

    private fun dummyWindows(n: Int) = (0 until n).map {
        WindowWithDetail(
            Window(0L, StudyDay(0), EpochMs(0), EpochMs(3_600_000L), WindowSource.INFERRED, false, true, null),
            emptyList(), emptyList(), 0.0, 0,
        )
    }

    private fun candidate(kind: SuggestionKind, impactShare: Double, subject: Pkg? = null) = Candidate(
        kind, subject, impactShare, TargetMetric.STAYS_PER_HOUR_FROM_PKG, emptyMap(), ActionType.NONE,
    )

    private fun baseCtx(
        validDayCount: Int = 10,
        windowCount: Int = 15,
        isFirstMirrorAfterLapse: Boolean = false,
        currentWeekNumber: Int = 5,
        experiments: List<Experiment> = emptyList(),
        suggestionStates: List<SuggestionState> = emptyList(),
    ) = SuggestionContext(
        windows = dummyWindows(windowCount), week = WeekSummary(
            weekStart, null, null, null, null, null, null, null, null, windowCount, validDayCount, false, 0, 0.0, 0,
        ),
        priorWeeks = emptyList(), days = validDays(validDayCount), baseline = null, patterns = emptyList(),
        clearHour = null, lapse = null, isFirstMirrorAfterLapse = isFirstMirrorAfterLapse,
        currentWeekNumber = currentWeekNumber, notificationAccessGranted = true, listenerCoverageFraction14d = 1.0,
        suggestionStates = suggestionStates, experiments = experiments, asOf = EpochMs(0), zone = zone,
    )

    @Test
    fun `G-G1 7 valid days is NOT_ENOUGH_DATA; 8 days and 9 windows still is; 8 and 10 is eligible`() {
        val candidates = listOf(candidate(SuggestionKind.S2, 0.5))

        val r1 = SuggestionSelector.select(baseCtx(validDayCount = 7, windowCount = 15), candidates)
        assertEquals(SilenceReason.NOT_ENOUGH_DATA, r1.reason)

        val r2 = SuggestionSelector.select(baseCtx(validDayCount = 8, windowCount = 9), candidates)
        assertEquals(SilenceReason.NOT_ENOUGH_DATA, r2.reason)

        val r3 = SuggestionSelector.select(baseCtx(validDayCount = 8, windowCount = 10), candidates)
        assertNull(r3.reason)
        assertEquals(SuggestionKind.S2, r3.task?.kind)
    }

    @Test
    fun `G-G2 a live experiment silences the task even if S2 would fire`() {
        val liveExperiment = Experiment(
            1L, SuggestionKind.S4, null, EpochMs(0), StartReason.TAP, TargetMetric.PINGS_FROM_PKG_IN_WINDOWS,
            null, null, EpochMs(0), Verdict.PENDING, false, true,
        )
        val candidates = listOf(candidate(SuggestionKind.S2, 0.9))

        val result = SuggestionSelector.select(baseCtx(experiments = listOf(liveExperiment)), candidates)

        assertNull(result.task)
        assertEquals(SilenceReason.EXPERIMENT_LIVE, result.reason)
    }

    @Test
    fun `G-G3 the first Mirror after a 3-day lapse has no task but shows the S11 observation`() {
        val candidates = listOf(
            candidate(SuggestionKind.S11, 0.0),
            candidate(SuggestionKind.S2, 0.9),
        )

        val result = SuggestionSelector.select(baseCtx(isFirstMirrorAfterLapse = true), candidates)

        assertNull(result.task)
        assertEquals(SilenceReason.AFTER_LAPSE, result.reason)
        assertEquals(SuggestionKind.S11, result.observation?.kind)
    }

    @Test
    fun `G-G4 ranking by impact, then never-shown, then lower kind number, then week 8 verdict multiplier`() {
        // S3 (0.62) beats S2 (0.40) outright.
        val r1 = SuggestionSelector.select(baseCtx(), listOf(candidate(SuggestionKind.S2, 0.40), candidate(SuggestionKind.S3, 0.62)))
        assertEquals(SuggestionKind.S3, r1.task?.kind)

        // Tie at 0.50: S2 never shown, S3 shown before -> S2 wins. shownInWeek is set safely in
        // the past (not the current week) so this doesn't also trip the global QUOTA check.
        val longAgo = WeekStart(StudyDay(weekStart.studyDay.epochDay - 70))
        val s3Shown = SuggestionState(SuggestionKind.S3, null, null, EpochMs(0), longAgo, null, null, "ACTIVE")
        val r2 = SuggestionSelector.select(
            baseCtx(suggestionStates = listOf(s3Shown)),
            listOf(candidate(SuggestionKind.S2, 0.50), candidate(SuggestionKind.S3, 0.50)),
        )
        assertEquals(SuggestionKind.S2, r2.task?.kind)

        // Tie at 0.50, neither ever shown -> lower kind number (S2) wins.
        val r3 = SuggestionSelector.select(baseCtx(), listOf(candidate(SuggestionKind.S2, 0.50), candidate(SuggestionKind.S3, 0.50)))
        assertEquals(SuggestionKind.S2, r3.task?.kind)

        // Week 9: S2 judged MOVED earlier (0.40 x 1.25 = 0.50) against S3's unadjusted 0.45 -> S2 wins.
        val s2Moved = Experiment(
            1L, SuggestionKind.S2, null, EpochMs(0), StartReason.TAP, TargetMetric.STAYS_PER_HOUR_FROM_PKG,
            null, null, EpochMs(0), Verdict.MOVED, false, true,
        )
        val r4 = SuggestionSelector.select(
            baseCtx(currentWeekNumber = 9, experiments = listOf(s2Moved)),
            listOf(candidate(SuggestionKind.S2, 0.40), candidate(SuggestionKind.S3, 0.45)),
        )
        assertEquals(SuggestionKind.S2, r4.task?.kind)
    }

    @Test
    fun `G-G5 dismissing S2 for pkgX blocks it until 4 weeks later, pkgY is unaffected`() {
        val pkgX = Pkg("X")
        val pkgY = Pkg("Y")
        val dismissedUntil = EpochMs(4L * 7 * 24 * 60 * 60 * 1_000) // 4 weeks after asOf=0
        val states = listOf(SuggestionState(SuggestionKind.S2, pkgX, null, null, null, dismissedUntil, null, "DISMISSED"))

        val result = SuggestionSelector.select(
            baseCtx(suggestionStates = states),
            listOf(candidate(SuggestionKind.S2, 0.9, pkgX), candidate(SuggestionKind.S2, 0.5, pkgY)),
        )

        assertEquals(pkgY, result.task?.subject) // pkgX is dismissed, pkgY survives and wins by default
    }

    @Test
    fun `G-G6 week 8 - a task shown in week 7 is QUOTA-blocked; shown in week 6 is eligible`() {
        val week7 = WeekStart(StudyDay(weekStart.studyDay.epochDay - 7))
        val week6 = WeekStart(StudyDay(weekStart.studyDay.epochDay - 14))
        val candidates = listOf(candidate(SuggestionKind.S2, 0.9))

        val shownWeek7 = listOf(SuggestionState(SuggestionKind.S1, null, null, EpochMs(0), week7, null, null, "ACTIVE"))
        val r1 = SuggestionSelector.select(baseCtx(currentWeekNumber = 8, suggestionStates = shownWeek7), candidates)
        assertEquals(SilenceReason.QUOTA, r1.reason)

        val shownWeek6 = listOf(SuggestionState(SuggestionKind.S1, null, null, EpochMs(0), week6, null, null, "ACTIVE"))
        val r2 = SuggestionSelector.select(baseCtx(currentWeekNumber = 8, suggestionStates = shownWeek6), candidates)
        assertNull(r2.reason)
    }

    @Test
    fun `property - at most one task per Mirror`() {
        val candidates = listOf(candidate(SuggestionKind.S1, 0.9), candidate(SuggestionKind.S2, 0.8), candidate(SuggestionKind.S3, 0.7))
        val result = SuggestionSelector.select(baseCtx(), candidates)
        assertTrue(result.task != null)
        // task is a single Candidate by type, not a list -- this is structurally guaranteed, not just by this test.
    }

    @Test
    fun `property - selector output is identical on identical input`() {
        val candidates = listOf(candidate(SuggestionKind.S2, 0.6), candidate(SuggestionKind.S3, 0.4))
        val ctx = baseCtx()
        assertEquals(SuggestionSelector.select(ctx, candidates), SuggestionSelector.select(ctx, candidates))
    }
}
