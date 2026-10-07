package com.kleos.sakshi.engine.metrics

import com.kleos.sakshi.engine.model.EndedBy
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.ScreenSpan
import com.kleos.sakshi.engine.model.Stay
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.model.WindowSource
import com.kleos.sakshi.engine.testkit.GOLDEN_DATE
import com.kleos.sakshi.engine.testkit.epochMsAt
import org.junit.Assert.assertEquals
import org.junit.Test

/** D1's stretch rules (DOC 3, F5. Steadiness and Its Four Parts). */
class StretchBuilderTest {
    private val day = StudyDay(GOLDEN_DATE.toEpochDay())

    private fun window(start: String, end: String) = Window(
        id = 1L, day = day, start = epochMsAt(start), end = epochMsAt(end),
        source = WindowSource.INFERRED, partial = false, finalised = true, shape = null,
    )

    @Test
    fun `D1 example - 25 min study plus 5 min screen-off is one 30-minute PUT_DOWN stretch`() {
        // Window continues to 10:35 so the quiet span does NOT run through to window end.
        val w = window("10:00:00", "10:35:00")
        val screenOff = listOf(ScreenSpan(epochMsAt("10:25:00"), epochMsAt("10:30:00")))

        val stretches = StretchBuilder.build(w, emptyList(), screenOff)

        val first = stretches.first()
        assertEquals(epochMsAt("10:00:00"), first.start)
        assertEquals(epochMsAt("10:30:00"), first.end)
        assertEquals(EndedBy.PUT_DOWN, first.endedBy)
        assertEquals(30.0, first.minutes, 0.0001)
        assertEquals(25.0, first.inSetMinutes, 0.0001)
        assertEquals(5.0, first.quietMinutes, 0.0001)
    }

    @Test
    fun `a 2-minute screen-off does not end a stretch`() {
        val w = window("10:00:00", "10:20:00")
        val screenOff = listOf(ScreenSpan(epochMsAt("10:10:00"), epochMsAt("10:12:00")))

        val stretches = StretchBuilder.build(w, emptyList(), screenOff)

        assertEquals(1, stretches.size)
        val only = stretches.single()
        assertEquals(epochMsAt("10:00:00"), only.start)
        assertEquals(epochMsAt("10:20:00"), only.end)
        assertEquals(EndedBy.WINDOW_END, only.endedBy)
        assertEquals(2.0, only.quietMinutes, 0.0001)
        assertEquals(18.0, only.inSetMinutes, 0.0001)
    }

    @Test
    fun `a window of only quiet is one stretch ended by WINDOW_END`() {
        val w = window("10:00:00", "10:10:00")
        val screenOff = listOf(ScreenSpan(epochMsAt("10:00:00"), epochMsAt("10:10:00")))

        val stretches = StretchBuilder.build(w, emptyList(), screenOff)

        assertEquals(1, stretches.size)
        val only = stretches.single()
        assertEquals(EndedBy.WINDOW_END, only.endedBy)
        assertEquals(10.0, only.quietMinutes, 0.0001)
        assertEquals(0.0, only.inSetMinutes, 0.0001)
    }

    @Test
    fun `a stay splits the window into a STAY-ended stretch and a WINDOW_END stretch`() {
        val w = window("10:00:00", "10:30:00")
        val stay = Stay(
            id = 0L, windowId = 1L, start = epochMsAt("10:10:00"), end = epochMsAt("10:11:00"),
            firstPkg = Pkg("B"), pkgMain = Pkg("B"), origin = Origin.UNKNOWN,
            stonePkg = null, notifClicked = false, returnMinutes = null, glancesBefore = 0,
        )

        val stretches = StretchBuilder.build(w, listOf(stay), emptyList())

        assertEquals(2, stretches.size)
        assertEquals(epochMsAt("10:00:00"), stretches[0].start)
        assertEquals(epochMsAt("10:10:00"), stretches[0].end)
        assertEquals(EndedBy.STAY, stretches[0].endedBy)
        assertEquals(10.0, stretches[0].minutes, 0.0001)

        assertEquals(epochMsAt("10:11:00"), stretches[1].start)
        assertEquals(epochMsAt("10:30:00"), stretches[1].end)
        assertEquals(EndedBy.WINDOW_END, stretches[1].endedBy)
        assertEquals(19.0, stretches[1].minutes, 0.0001)
    }
}
