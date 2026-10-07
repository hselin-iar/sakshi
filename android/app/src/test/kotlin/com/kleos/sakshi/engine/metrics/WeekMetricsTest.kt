package com.kleos.sakshi.engine.metrics

import com.kleos.sakshi.engine.model.EndedBy
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.Stay
import com.kleos.sakshi.engine.model.Stretch
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.model.WindowSource
import org.junit.Assert.assertEquals
import org.junit.Test

/** G-F5 and G-F6 are the spec (DOC 3, F5. Steadiness and Its Four Parts: pooling). */
class WeekMetricsTest {

    private fun stretch(minutes: Double) = Stretch(
        id = 0L, windowId = 0L, start = EpochMs(0), end = EpochMs((minutes * 60_000).toLong()),
        minutes = minutes, inSetMinutes = minutes, quietMinutes = 0.0, endedBy = EndedBy.WINDOW_END,
    )

    private fun windowOfHours(hours: Double) = Window(
        id = 0L, day = StudyDay(0), start = EpochMs(0), end = EpochMs((hours * 3_600_000).toLong()),
        source = WindowSource.INFERRED, partial = false, finalised = true, shape = null,
    )

    private fun dummyStay() = Stay(
        id = 0L, windowId = 0L, start = EpochMs(0), end = EpochMs(0),
        firstPkg = Pkg("X"), pkgMain = Pkg("X"), origin = Origin.UNKNOWN,
        stonePkg = null, notifClicked = false, returnMinutes = null, glancesBefore = 0,
    )

    @Test
    fun `G-F5 length-weighted median of 30,10,10,10,10 is 10`() {
        val stretches = listOf(30.0, 10.0, 10.0, 10.0, 10.0).map { stretch(it) }

        val parts = WeekMetrics.pool(stretches, emptyList(), emptyList())

        assertEquals(10.0, parts.stretchMin!!, 0.0001)
    }

    @Test
    fun `G-F6a zero stays over 5 window-hours smooths to 1-6`() {
        val windows = listOf(windowOfHours(5.0))

        val parts = WeekMetrics.pool(emptyList(), emptyList(), windows)

        assertEquals(1.0 / 6.0, parts.staysPerHour!!, 0.0001)
    }

    @Test
    fun `G-F6b six stays over 5 window-hours smooths to 7-6`() {
        val windows = listOf(windowOfHours(5.0))
        val stays = (1..6).map { dummyStay() }

        val parts = WeekMetrics.pool(emptyList(), stays, windows)

        assertEquals(7.0 / 6.0, parts.staysPerHour!!, 0.0001)
    }

    @Test
    fun `returnMin is the median of resolved returns only, unresolved stays excluded`() {
        val stays = listOf(
            dummyStay().copy(returnMinutes = 2.0),
            dummyStay().copy(returnMinutes = 4.0),
            dummyStay().copy(returnMinutes = 6.0),
            dummyStay().copy(returnMinutes = null), // unresolved, excluded
        )

        val parts = WeekMetrics.pool(emptyList(), stays, emptyList())

        assertEquals(4.0, parts.returnMin!!, 0.0001)
    }
}
