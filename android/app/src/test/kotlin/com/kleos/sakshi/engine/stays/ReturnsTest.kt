package com.kleos.sakshi.engine.stays

import com.kleos.sakshi.engine.model.AppClass
import com.kleos.sakshi.engine.model.ForegroundInterval
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.ScreenSpan
import com.kleos.sakshi.engine.model.Stay
import com.kleos.sakshi.engine.testkit.epochMsAt
import org.junit.Assert.assertEquals
import org.junit.Test

/** G-S6a and G-S6b are the spec (DOC 3, F4. Stone and Wave: Returns). */
class ReturnsTest {

    private fun stay(start: String, end: String) = Stay(
        id = 0L, windowId = 1L, start = epochMsAt(start), end = epochMsAt(end),
        firstPkg = Pkg("B"), pkgMain = Pkg("B"), origin = Origin.UNKNOWN,
        stonePkg = null, notifClicked = false, returnMinutes = null, glancesBefore = 0,
    )

    @Test
    fun `G-S6a back in-set for 40s at 10-14-00 returns 4 minutes`() {
        val theStay = stay("10:10:00", "10:10:30")
        val intervals = listOf(ForegroundInterval(Pkg("A"), epochMsAt("10:14:00"), epochMsAt("10:14:40")))
        val classes = listOf(AppClass.IN_SET)

        val minutes = Returns.returnMinutes(theStay, intervals, classes, emptyList(), epochMsAt("11:00:00"))

        assertEquals(4.0, minutes!!, 0.0001)
    }

    @Test
    fun `G-S6b a 8-minute screen-off put-down at 10-12-00 returns 2 minutes`() {
        val theStay = stay("10:10:00", "10:10:30")
        val screenOff = listOf(ScreenSpan(epochMsAt("10:12:00"), epochMsAt("10:20:00")))

        val minutes = Returns.returnMinutes(theStay, emptyList(), emptyList(), screenOff, epochMsAt("11:00:00"))

        assertEquals(2.0, minutes!!, 0.0001)
    }

    @Test
    fun `neither an in-set return nor a put-down before window end is unresolved`() {
        val theStay = stay("10:10:00", "10:10:30")

        val minutes = Returns.returnMinutes(theStay, emptyList(), emptyList(), emptyList(), epochMsAt("10:20:00"))

        assertEquals(null, minutes)
    }

    @Test
    fun `a put-down earlier than a qualifying in-set return wins`() {
        val theStay = stay("10:10:00", "10:10:30")
        // In-set return at 10:14:00 (4 min) vs put-down at 10:11:00 (2 min after stay.end) -- put-down is earlier.
        val intervals = listOf(ForegroundInterval(Pkg("A"), epochMsAt("10:14:00"), epochMsAt("10:14:40")))
        val classes = listOf(AppClass.IN_SET)
        val screenOff = listOf(ScreenSpan(epochMsAt("10:11:00"), epochMsAt("10:19:00")))

        val minutes = Returns.returnMinutes(theStay, intervals, classes, screenOff, epochMsAt("11:00:00"))

        assertEquals(1.0, minutes!!, 0.0001) // 10:11:00 - 10:10:00
    }
}
