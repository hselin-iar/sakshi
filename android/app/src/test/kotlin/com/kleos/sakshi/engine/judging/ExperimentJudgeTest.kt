package com.kleos.sakshi.engine.judging

import com.kleos.sakshi.engine.judging.JudgeFixtures.afterStarts
import com.kleos.sakshi.engine.judging.JudgeFixtures.baseline
import com.kleos.sakshi.engine.judging.JudgeFixtures.beforeStarts
import com.kleos.sakshi.engine.judging.JudgeFixtures.daysOf
import com.kleos.sakshi.engine.judging.JudgeFixtures.end
import com.kleos.sakshi.engine.judging.JudgeFixtures.experiment
import com.kleos.sakshi.engine.judging.JudgeFixtures.monday
import com.kleos.sakshi.engine.judging.JudgeFixtures.started
import com.kleos.sakshi.engine.judging.JudgeFixtures.week
import com.kleos.sakshi.engine.judging.JudgeFixtures.window
import com.kleos.sakshi.engine.judging.JudgeFixtures.windows
import com.kleos.sakshi.engine.judging.JudgeFixtures.zone
import com.kleos.sakshi.engine.model.Baseline
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.TargetMetric
import com.kleos.sakshi.engine.model.Verdict
import com.kleos.sakshi.engine.model.WeekSummary
import com.kleos.sakshi.engine.model.WindowWithDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** G-J1 .. G-J7 are the spec (DOC 3, F12. Self-Judging Verdicts). */
class ExperimentJudgeTest {

    private fun ctx(
        before: List<WindowWithDetail>, after: List<WindowWithDetail>, baseline: Baseline? = null,
        weeks: List<WeekSummary> = emptyList(), installed: Boolean = true, asOf: EpochMs = end,
    ) = JudgeContext(
        windows = before + after, days = daysOf(before, after), weeks = weeks, baseline = baseline,
        subjectInstalled = installed, asOf = asOf, zone = zone,
    )

    /** `staysTotal` stays spread over `afterStarts` windows: n/10 each (all tests use multiples of 10 -> exact rates). */
    private fun period(starts: List<EpochMs>, totalStays: Int, n: Int = starts.size, stretch: Double = 10.0, ret: Double = 6.0) =
        windows(starts, n, perWindow = { i -> totalStays / n + if (i < totalStays % n) 1 else 0 }) { s, k ->
            window(s, k, stretchMin = stretch, returnMin = ret)
        }

    @Test fun `G-J1 stays per hour from the app 4_1 to 3_0 is a 26_8 percent improvement and MOVED`() {
        val out = ExperimentJudge.outcome(experiment(), ctx(period(beforeStarts, 41), period(afterStarts, 30), baseline))
        assertEquals(Verdict.MOVED, out.verdict)
        assertEquals(4.1, out.beforeValue!!, 1e-9)
        assertEquals(3.0, out.afterValue!!, 1e-9)
        assertFalse(out.approxMix)
    }

    @Test fun `G-J2 3_1 to 2_7 is 12_9 percent and NO_CHANGE`() {
        val out = ExperimentJudge.outcome(experiment(), ctx(period(beforeStarts, 31), period(afterStarts, 27), baseline))
        assertEquals(Verdict.NO_CHANGE, out.verdict)
        assertEquals(3.1, out.beforeValue!!, 1e-9)
        assertEquals(2.7, out.afterValue!!, 1e-9)
    }

    @Test fun `G-J3 4_1 to 3_0 but Steadiness fell is NO_CHANGE`() {
        // after: shorter stretches and slower returns pull Steadiness below the before-period's, whatever the target did
        val before = period(beforeStarts, 41)
        val after = period(afterStarts, 30, stretch = 5.0, ret = 12.0)
        assertEquals(Verdict.NO_CHANGE, ExperimentJudge.judge(experiment(), ctx(before, after, baseline)))
        // the same data with no baseline cannot say Steadiness fell, so the improvement stands
        assertEquals(Verdict.MOVED, ExperimentJudge.judge(experiment(), ctx(before, after, baseline = null)))
    }

    @Test fun `G-J4 seven windows in the after period is TOO_LITTLE and eight is judged`() {
        val before = period(beforeStarts, 41)
        assertEquals(Verdict.TOO_LITTLE, ExperimentJudge.judge(experiment(), ctx(before, period(afterStarts, 21, n = 7))))
        assertEquals(Verdict.MOVED, ExperimentJudge.judge(experiment(), ctx(before, period(afterStarts, 24, n = 8))))
    }

    @Test fun `G-J5 an unusual week inside the after period is UNCLEAR and wins over TOO_LITTLE`() {
        val before = period(beforeStarts, 41)
        val tooFew = period(afterStarts, 21, n = 7)
        val unusualAfter = listOf(week(monday.plusDays(7), unusual = true))
        assertEquals(Verdict.UNCLEAR, ExperimentJudge.judge(experiment(), ctx(before, tooFew, weeks = unusualAfter)))
        // an unusual week in the before period counts too
        val unusualBefore = listOf(week(monday.minusDays(7), unusual = true))
        assertEquals(Verdict.UNCLEAR, ExperimentJudge.judge(experiment(), ctx(before, period(afterStarts, 30), weeks = unusualBefore)))
        // an unusual week well outside both periods does not
        val unrelated = listOf(week(monday.minusDays(28), unusual = true), week(monday.plusDays(21), unusual = true))
        assertEquals(Verdict.MOVED, ExperimentJudge.judge(experiment(), ctx(before, period(afterStarts, 30), weeks = unrelated)))
        // a usual week overlapping the periods is no reason for doubt
        assertEquals(Verdict.MOVED, ExperimentJudge.judge(experiment(), ctx(before, period(afterStarts, 30), weeks = listOf(week(monday, unusual = false)))))
    }

    @Test fun `G-J6 higher is better - stretch in slot 10 to 12_5 is MOVED and 10 to 11_5 is NO_CHANGE`() {
        val exp = experiment(target = TargetMetric.STRETCH_IN_SLOT, subject = null)
        val before = period(beforeStarts, 20, stretch = 10.0)
        val moved = ExperimentJudge.outcome(exp, ctx(before, period(afterStarts, 20, stretch = 12.5)))
        assertEquals(Verdict.MOVED, moved.verdict)
        assertEquals(10.0, moved.beforeValue!!, 1e-9); assertEquals(12.5, moved.afterValue!!, 1e-9)
        assertEquals(Verdict.NO_CHANGE, ExperimentJudge.judge(exp, ctx(before, period(afterStarts, 20, stretch = 11.5))))
    }

    @Test fun `G-J7 weekday mix - equal real behaviour shows no improvement (the naive pooled before of 4_5 would)`() {
        // before: 6 weekday windows at 5.0 stays/h, 2 weekend windows at 3.0.  after: 2 weekday at 5.0, 6 weekend at 3.0.
        val before = mixed(weekdays = 6, weekdayStays = 5, weekends = 2, weekendStays = 3, firstMonday = monday.minusDays(14))
        val after = mixed(weekdays = 2, weekdayStays = 5, weekends = 6, weekendStays = 3, firstMonday = monday)
        val out = ExperimentJudge.outcome(experiment(), ctx(before, after))
        assertEquals(3.5, out.beforeValue!!, 1e-9)       // re-weighted: 0.25 * 5.0 + 0.75 * 3.0
        assertEquals(3.5, out.afterValue!!, 1e-9)
        assertEquals(Verdict.NO_CHANGE, out.verdict)
        assertFalse(out.approxMix)
        // what pooling without the re-weighting would have claimed
        val naive = TargetMetrics.value(TargetMetric.STAYS_PER_HOUR_FROM_PKG, experiment().subject, before)!!
        assertEquals(4.5, naive, 1e-9)
        assertTrue((naive - 3.5) / naive >= 0.20)
    }

    // ---- precedence and properties ----

    @Test fun `PENDING until exactly 14 days, judged at 14 days`() {
        val before = period(beforeStarts, 41); val after = period(afterStarts, 30)
        val oneMsEarly = EpochMs(end.value - 1)
        assertEquals(Verdict.PENDING, ExperimentJudge.judge(experiment(), ctx(before, after, asOf = oneMsEarly)))
        assertEquals(Verdict.PENDING, ExperimentJudge.judge(experiment(), ctx(before, after, asOf = started)))
        assertNotEquals(Verdict.PENDING, ExperimentJudge.judge(experiment(), ctx(before, after, asOf = end)))
    }

    @Test fun `PENDING comes before UNCLEAR - an unusual week cannot judge an experiment still running`() {
        val c = ctx(period(beforeStarts, 41), period(afterStarts, 30), weeks = listOf(week(monday, unusual = true)), asOf = EpochMs(end.value - 1))
        assertEquals(Verdict.PENDING, ExperimentJudge.judge(experiment(), c))
    }

    @Test fun `a verdict never changes once written`() {
        val goodData = ctx(period(beforeStarts, 41), period(afterStarts, 30))
        listOf(Verdict.MOVED, Verdict.NO_CHANGE, Verdict.TOO_LITTLE, Verdict.UNCLEAR).forEach { v ->
            val written = experiment(verdict = v).copy(beforeValue = 1.0, afterValue = 2.0, approxMix = true)
            val out = ExperimentJudge.outcome(written, goodData)
            assertEquals(v, out.verdict)
            assertEquals(1.0, out.beforeValue); assertEquals(2.0, out.afterValue); assertTrue(out.approxMix)
        }
    }

    @Test fun `an uninstalled subject app is UNCLEAR, but an experiment with no subject does not care`() {
        val before = period(beforeStarts, 41); val after = period(afterStarts, 30)
        assertEquals(Verdict.UNCLEAR, ExperimentJudge.judge(experiment(), ctx(before, after, installed = false)))
        val noSubject = experiment(target = TargetMetric.MEDIAN_RETURN, subject = null)
        assertNotEquals(Verdict.UNCLEAR, ExperimentJudge.judge(noSubject, ctx(before, after, installed = false)))
    }

    @Test fun `a missing metric on either side is TOO_LITTLE with no zero comparison`() {
        val exp = experiment(target = TargetMetric.MEDIAN_RETURN, subject = null)
        val noReturns = windows(afterStarts, perWindow = { 2 }) { s, k -> window(s, k, returnMin = 6.0).let { w -> w.copy(stays = w.stays.map { st -> st.copy(returnMinutes = null) }) } }
        val out = ExperimentJudge.outcome(exp, ctx(period(beforeStarts, 20), noReturns))
        assertEquals(Verdict.TOO_LITTLE, out.verdict)
        assertNull(out.afterValue)
    }

    @Test fun `partial windows and windows on invalid days are left out`() {
        val before = period(beforeStarts, 41)
        val rawAfter = period(afterStarts, 30)
        // two of ten become partial -> 8 left, still judged; three partial -> 7 left, TOO_LITTLE
        fun partial(n: Int) = rawAfter.mapIndexed { i, w -> if (i < n) w.copy(window = w.window.copy(partial = true)) else w }
        assertNotEquals(Verdict.TOO_LITTLE, ExperimentJudge.judge(experiment(), ctx(before, partial(2))))
        assertEquals(Verdict.TOO_LITTLE, ExperimentJudge.judge(experiment(), ctx(before, partial(3))))
        // a day that is not valid takes its windows out of the judging
        val invalidDays = daysOf(before, rawAfter).mapIndexed { i, d -> if (d.day == rawAfter[0].window.day || d.day == rawAfter[1].window.day || d.day == rawAfter[2].window.day) d.copy(valid = false) else d }
        val c = JudgeContext(before + rawAfter, invalidDays, emptyList(), null, true, end, zone)
        assertEquals(Verdict.TOO_LITTLE, ExperimentJudge.judge(experiment(), c))
    }

    @Test fun `the improvement boundary is inclusive at exactly 20 percent`() {
        assertEquals(Verdict.MOVED, ExperimentJudge.judge(experiment(), ctx(period(beforeStarts, 50), period(afterStarts, 40))))
        assertEquals(Verdict.NO_CHANGE, ExperimentJudge.judge(experiment(), ctx(period(beforeStarts, 50), period(afterStarts, 41))))
    }

    @Test fun `nothing to improve from zero is NO_CHANGE and never a crash`() {
        assertEquals(Verdict.NO_CHANGE, ExperimentJudge.judge(experiment(), ctx(period(beforeStarts, 0), period(afterStarts, 0))))
        assertEquals(Verdict.NO_CHANGE, ExperimentJudge.judge(experiment(), ctx(period(beforeStarts, 0), period(afterStarts, 10))))
    }

    @Test fun `a class missing on one side falls back to pooled values and says so`() {
        // before has only weekdays; after has weekend windows too
        val before = period(beforeStarts, 41)
        val after = mixed(weekdays = 4, weekdayStays = 3, weekends = 4, weekendStays = 3, firstMonday = monday)
        val out = ExperimentJudge.outcome(experiment(), ctx(before, after))
        assertTrue(out.approxMix)
        assertEquals(4.1, out.beforeValue!!, 1e-9)      // pooled, not re-weighted
        assertEquals(Verdict.MOVED, out.verdict)
    }

    @Test fun `the same input always gives the same outcome`() {
        val c = ctx(period(beforeStarts, 41), period(afterStarts, 30), baseline)
        assertEquals(ExperimentJudge.outcome(experiment(), c), ExperimentJudge.outcome(experiment(), c))
    }

    /** Windows on weekdays (Mon..Fri of `firstMonday`'s week, wrapping to the next) and on the weekend days that follow. */
    private fun mixed(weekdays: Int, weekdayStays: Int, weekends: Int, weekendStays: Int, firstMonday: java.time.LocalDate): List<WindowWithDetail> {
        fun at(date: java.time.LocalDate, hour: Int) = com.kleos.sakshi.engine.testkit.epochMs(date, java.time.LocalTime.of(hour, 0), zone)
        val weekdayDates = (0 until 14).map { firstMonday.plusDays(it.toLong()) }.filter { it.dayOfWeek.value <= 5 }
        val weekendDates = (0 until 14).map { firstMonday.plusDays(it.toLong()) }.filter { it.dayOfWeek.value >= 6 }
        val weekdaySlots = weekdayDates.map { at(it, 20) }
        val weekendSlots = weekendDates.flatMap { listOf(at(it, 15), at(it, 20)) }
        return weekdaySlots.take(weekdays).map { window(it, weekdayStays) } + weekendSlots.take(weekends).map { window(it, weekendStays) }
    }
}
