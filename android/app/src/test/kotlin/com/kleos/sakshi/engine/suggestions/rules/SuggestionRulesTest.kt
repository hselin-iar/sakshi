package com.kleos.sakshi.engine.suggestions.rules

import com.kleos.sakshi.engine.model.Baseline
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.Stay
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.WeekStart
import com.kleos.sakshi.engine.model.WeekSummary
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.model.WindowSource
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.suggestions.SuggestionContext
import com.kleos.sakshi.engine.testkit.Zones
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** G-G7's per-rule boundaries (DOC 3/DOC4, F11): S2 34%/35%, S6 49%/50%, S5 7.9min/8.0min. */
class SuggestionRulesTest {
    private val zone = Zones.KOLKATA
    private val day = StudyDay(0)

    private fun stay(firstPkg: Pkg) = Stay(0L, 0L, EpochMs(0), EpochMs(0), firstPkg, firstPkg, Origin.UNKNOWN, null, false, null, 0)

    private fun window(stays: List<Stay>) = WindowWithDetail(
        Window(0L, day, EpochMs(0), EpochMs(3_600_000L), WindowSource.INFERRED, false, true, null),
        emptyList(), stays, 0.0, 0,
    )

    private fun ctxWithStays(stays: List<Stay>, windowCount: Int = 8) = SuggestionContext(
        windows = List(windowCount) { window(if (it == 0) stays else emptyList()) },
        week = null, priorWeeks = emptyList(), days = emptyList(), baseline = null, patterns = emptyList(),
        clearHour = null, lapse = null, isFirstMirrorAfterLapse = false, currentWeekNumber = 5,
        notificationAccessGranted = true, listenerCoverageFraction14d = 1.0,
        suggestionStates = emptyList(), experiments = emptyList(), asOf = EpochMs(0), zone = zone,
    )

    @Test
    fun `S2 at 34pct does not fire, 35pct fires`() {
        val pkgA = Pkg("A")
        // The rule picks the pkg with the MOST stays, so the remainder must be split across two
        // other pkgs (33+33, 33+32) -- a single other pkg would itself dominate (66%/65%),
        // masking whatever share A actually has.
        val pkgB = Pkg("B")
        val pkgC = Pkg("C")

        // 34 of 100 stays from A: just under S2_SHARE (0.35).
        val stays34 = List(34) { stay(pkgA) } + List(33) { stay(pkgB) } + List(33) { stay(pkgC) }
        assertNull(S02RepeatLeak.evaluate(ctxWithStays(stays34)))

        // 35 of 100: at the boundary, inclusive.
        val stays35 = List(35) { stay(pkgA) } + List(33) { stay(pkgB) } + List(32) { stay(pkgC) }
        assertNotNull(S02RepeatLeak.evaluate(ctxWithStays(stays35)))
    }

    @Test
    fun `S6 at 49pct does not fire, 50pct fires`() {
        // 10 windows minimum; first-stay timing controls the flinch share.
        val flinchStay = Stay(0L, 0L, EpochMs(0), EpochMs(0), Pkg("A"), Pkg("A"), Origin.UNKNOWN, null, false, null, 0) // starts at window start -> flinch
        val lateStay = Stay(0L, 0L, EpochMs(10 * 60_000L), EpochMs(0), Pkg("A"), Pkg("A"), Origin.UNKNOWN, null, false, null, 0) // 10min in -> not a flinch

        fun ctxWithFlinchCount(flinchCount: Int, total: Int = 100): SuggestionContext {
            val windows = (0 until total).map { i ->
                val w = Window(0L, day, EpochMs(0), EpochMs(3_600_000L), WindowSource.INFERRED, false, true, null)
                val stayList = listOf(if (i < flinchCount) flinchStay else lateStay)
                WindowWithDetail(w, emptyList(), stayList, 0.0, 0)
            }
            return SuggestionContext(
                windows = windows, week = null, priorWeeks = emptyList(), days = emptyList(), baseline = null,
                patterns = emptyList(), clearHour = null, lapse = null, isFirstMirrorAfterLapse = false,
                currentWeekNumber = 5, notificationAccessGranted = true, listenerCoverageFraction14d = 1.0,
                suggestionStates = emptyList(), experiments = emptyList(), asOf = EpochMs(0), zone = zone,
            )
        }

        assertNull(S06EarlyFlinch.evaluate(ctxWithFlinchCount(49)))
        assertNotNull(S06EarlyFlinch.evaluate(ctxWithFlinchCount(50)))
    }

    @Test
    fun `S5 fires at the absolute 8-0 minute threshold even when the ratio alone would not qualify`() {
        val baseline = Baseline(0L, EpochMs(0), c0 = 1.0, p0 = 1.0, r0 = 10.0, q0 = 1.0, daysUsed = 8, isActive = true)
        // ratio = 7.9/10 = 0.79 and 8.0/10 = 0.80, both well under S5_RATIO (1.5) -- only the
        // absolute >= 8.0min condition can fire either of these.
        fun ctxWithMedian(median: Double) = SuggestionContext(
            windows = listOf(window(listOf(stay(Pkg("A")).copy(returnMinutes = median)))),
            week = WeekSummary(WeekStart(StudyDay(0)), null, null, null, median, null, null, null, null, 1, 1, false, 0, 0.0, 0),
            priorWeeks = emptyList(), days = emptyList(), baseline = baseline, patterns = emptyList(),
            clearHour = null, lapse = null, isFirstMirrorAfterLapse = false, currentWeekNumber = 5,
            notificationAccessGranted = true, listenerCoverageFraction14d = 1.0,
            suggestionStates = emptyList(), experiments = emptyList(), asOf = EpochMs(0), zone = zone,
        )

        assertNull(S05SlowReturn.evaluate(ctxWithMedian(7.9)))
        assertNotNull(S05SlowReturn.evaluate(ctxWithMedian(8.0)))
    }
}
