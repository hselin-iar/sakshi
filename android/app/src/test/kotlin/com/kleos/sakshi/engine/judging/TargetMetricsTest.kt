package com.kleos.sakshi.engine.judging

import com.kleos.sakshi.engine.judging.JudgeFixtures.afterStarts
import com.kleos.sakshi.engine.judging.JudgeFixtures.stay
import com.kleos.sakshi.engine.judging.JudgeFixtures.subject
import com.kleos.sakshi.engine.judging.JudgeFixtures.validDay
import com.kleos.sakshi.engine.judging.JudgeFixtures.window
import com.kleos.sakshi.engine.model.EndedBy
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.TargetMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetMetricsTest {
    private val other = Pkg("com.other")
    private val s = afterStarts

    @Test fun `exactly three targets are higher-is-better and every other is lower-is-better`() {
        val higher = setOf(TargetMetric.STRETCH_IN_SLOT, TargetMetric.NEXT_DAY_FIRST_STRETCH, TargetMetric.PUTDOWN_SHARE)
        TargetMetric.entries.forEach { assertEquals("$it", it in higher, TargetMetrics.higherIsBetter(it)) }
    }

    @Test fun `stays per hour from one package counts that package's first-pkg stays over window hours`() {
        val ws = listOf(
            window(s[0], 3, otherStays = listOf({ id -> stay(id, s[0].value + 20 * 60_000L, pkg = other) })),
            window(s[1], 1),
        )
        assertEquals(2.0, TargetMetrics.value(TargetMetric.STAYS_PER_HOUR_FROM_PKG, subject, ws)!!, 1e-9)   // 4 stays / 2 hours
        assertNull(TargetMetrics.value(TargetMetric.STAYS_PER_HOUR_FROM_PKG, null, ws))
        assertNull(TargetMetrics.value(TargetMetric.STAYS_PER_HOUR_FROM_PKG, subject, emptyList()))
    }

    @Test fun `self started per hour counts only SELF_STARTED stays`() {
        val ws = listOf(window(s[0], 2, otherStays = listOf({ id -> stay(id, s[0].value + 30 * 60_000L, origin = Origin.STONE, stone = subject) })))
        assertEquals(2.0, TargetMetrics.value(TargetMetric.SELF_STARTED_PER_HOUR, null, ws)!!, 1e-9)
    }

    @Test fun `pings from a package count the stays that began with its ping`() {
        val ws = listOf(
            window(s[0], 0, otherStays = listOf(
                { id -> stay(id, s[0].value + 5 * 60_000L, origin = Origin.STONE, stone = subject) },
                { id -> stay(id, s[0].value + 25 * 60_000L, origin = Origin.STONE, stone = subject) },
                { id -> stay(id, s[0].value + 45 * 60_000L, origin = Origin.STONE, stone = other) },
                { id -> stay(id, s[0].value + 50 * 60_000L, origin = Origin.UNKNOWN) },
            )),
        )
        assertEquals(2.0, TargetMetrics.value(TargetMetric.PINGS_FROM_PKG_IN_WINDOWS, subject, ws)!!, 1e-9)
    }

    @Test fun `median return ignores unresolved returns and is null when there are none`() {
        val ws = listOf(window(s[0], 3, returnMin = 4.0), window(s[1], 1, returnMin = 10.0))     // 4,4,4,10
        assertEquals(4.0, TargetMetrics.value(TargetMetric.MEDIAN_RETURN, null, ws)!!, 1e-9)
        val unresolved = ws.map { w -> w.copy(stays = w.stays.map { it.copy(returnMinutes = null) }) }
        assertNull(TargetMetrics.value(TargetMetric.MEDIAN_RETURN, null, unresolved))
    }

    @Test fun `flinch rate is the share of windows whose first stay is inside the first five minutes`() {
        val flinch = window(s[0], 2)                                                              // first stay at +0
        val late = window(s[1], 0, otherStays = listOf({ id -> stay(id, s[1].value + 10 * 60_000L) }))
        val none = window(s[2], 0)                                                                // no stays at all: not a flinch
        assertEquals(1.0 / 3.0, TargetMetrics.value(TargetMetric.FLINCH_RATE, null, listOf(flinch, late, none))!!, 1e-9)
        assertNull(TargetMetrics.value(TargetMetric.FLINCH_RATE, null, emptyList()))
    }

    @Test fun `work-set coverage is the package's stay time over window time`() {
        val ws = listOf(window(s[0], 6), window(s[1], 0))                                         // 6 one-minute stays in 120 minutes
        assertEquals(6.0 / 120.0, TargetMetrics.value(TargetMetric.WORKSET_COVERAGE, subject, ws)!!, 1e-9)
    }

    @Test fun `stretch in slot is the length-weighted median stretch of the supplied windows`() {
        val ws = listOf(window(s[0], 1, stretchMin = 30.0), window(s[1], 1, stretchMin = 10.0), window(s[2], 1, stretchMin = 10.0))
        assertEquals(30.0, TargetMetrics.value(TargetMetric.STRETCH_IN_SLOT, null, ws)!!, 1e-9)  // 30 of 50 minutes is already >= half
    }

    @Test fun `put-down share is put-downs among stretches that ended in a stay or a put-down`() {
        val w = window(s[0], 1)
        val stretches = listOf(
            JudgeFixtures.stretch(w.window.id, s[0].value, 5.0, 0.0, EndedBy.STAY),
            JudgeFixtures.stretch(w.window.id, s[0].value, 5.0, 0.0, EndedBy.PUT_DOWN),
            JudgeFixtures.stretch(w.window.id, s[0].value, 5.0, 0.0, EndedBy.PUT_DOWN),
            JudgeFixtures.stretch(w.window.id, s[0].value, 5.0, 0.0, EndedBy.WINDOW_END),   // neither: left out
        )
        assertEquals(2.0 / 3.0, TargetMetrics.value(TargetMetric.PUTDOWN_SHARE, null, listOf(w.copy(stretches = stretches)))!!, 1e-9)
        assertNull(TargetMetrics.value(TargetMetric.PUTDOWN_SHARE, null, listOf(w.copy(stretches = emptyList()))))
    }

    @Test fun `next-day first stretch is the mean first stretch over the days given`() {
        val days = listOf(validDay(StudyDay(1), 6.0), validDay(StudyDay(2), 10.0), validDay(StudyDay(3), null))
        assertEquals(8.0, TargetMetrics.value(TargetMetric.NEXT_DAY_FIRST_STRETCH, null, emptyList(), days)!!, 1e-9)
        assertNull(TargetMetrics.value(TargetMetric.NEXT_DAY_FIRST_STRETCH, null, emptyList(), listOf(validDay(StudyDay(3), null))))
    }

    @Test fun `no value is ever zero for lack of data`() {
        TargetMetric.entries.forEach { assertNull("$it with nothing to measure", TargetMetrics.value(it, subject, emptyList())) }
        assertFalse(TargetMetrics.higherIsBetter(TargetMetric.MEDIAN_RETURN))
        assertTrue(TargetMetrics.higherIsBetter(TargetMetric.PUTDOWN_SHARE))
    }
}
