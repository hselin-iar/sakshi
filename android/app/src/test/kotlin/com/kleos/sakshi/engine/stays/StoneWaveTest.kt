package com.kleos.sakshi.engine.stays

import com.kleos.sakshi.engine.model.AppClass
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.NotifEvent
import com.kleos.sakshi.engine.model.NotifKind
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.RemovalKind
import com.kleos.sakshi.engine.model.Stay
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.model.WindowSource
import com.kleos.sakshi.engine.testkit.FakeListenerCoverage
import com.kleos.sakshi.engine.testkit.GOLDEN_DATE
import com.kleos.sakshi.engine.testkit.epochMsAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** G-S4 and G-S5 are the spec (DOC 3, F4. Stone and Wave: StoneWave). */
class StoneWaveTest {
    private val day = StudyDay(GOLDEN_DATE.toEpochDay())

    private fun stayAt(start: String, firstPkg: String = "B") = Stay(
        id = 0L, windowId = 1L, start = epochMsAt(start), end = epochMsAt(start),
        firstPkg = Pkg(firstPkg), pkgMain = Pkg(firstPkg), origin = Origin.UNKNOWN,
        stonePkg = null, notifClicked = false, returnMinutes = null, glancesBefore = 0,
    )

    private fun posted(pkg: String, ts: String) =
        NotifEvent(epochMsAt(ts), Pkg(pkg), category = null, kind = NotifKind.POSTED, removal = null, ongoing = false)

    private fun clicked(pkg: String, ts: String) =
        NotifEvent(epochMsAt(ts), Pkg(pkg), category = null, kind = NotifKind.REMOVED, removal = RemovalKind.CLICK, ongoing = false)

    private fun fullCoverage() = FakeListenerCoverage().apply {
        openSession(epochMsAt("00:00:00"))
    }

    @Test
    fun `G-S4a a ping 15s before the stay is STONE`() {
        val stay = stayAt("10:10:00")
        val notifs = listOf(posted("B", "10:09:45"))

        val result = StoneWave.classify(stay, notifs, fullCoverage())

        assertEquals(Origin.STONE, result.origin)
        assertEquals(Pkg("B"), result.stonePkg)
    }

    @Test
    fun `G-S4b a ping 40s before the stay (outside the 30s look-back) is SELF_STARTED`() {
        val stay = stayAt("10:10:00")
        val notifs = listOf(posted("B", "10:09:20"))

        val result = StoneWave.classify(stay, notifs, fullCoverage())

        assertEquals(Origin.SELF_STARTED, result.origin)
        assertNull(result.stonePkg)
    }

    @Test
    fun `G-S4c a click 13s after the ping is detected`() {
        val stay = stayAt("10:10:00")
        val notifs = listOf(posted("B", "10:09:45"), clicked("B", "10:09:58"))

        val result = StoneWave.classify(stay, notifs, fullCoverage())

        assertEquals(Origin.STONE, result.origin)
        assertTrue(result.notifClicked)
    }

    @Test
    fun `G-S5 a coverage hole over the look-back is UNKNOWN`() {
        val stay = stayAt("10:10:00")
        val notifs = listOf(posted("B", "10:09:45"))
        // Coverage connects, then disconnects over 10:09:30-10:09:50 (inside the look-back), then reconnects.
        val coverage = FakeListenerCoverage().apply {
            openSession(epochMsAt("10:00:00"))
            closeSession(epochMsAt("10:09:30"))
            openSession(epochMsAt("10:09:50"))
        }

        val result = StoneWave.classify(stay, notifs, coverage)

        assertEquals(Origin.UNKNOWN, result.origin)
    }

    @Test
    fun `a ping from app B then a stay in app C is SELF_STARTED, not STONE`() {
        val stay = stayAt("10:10:00", firstPkg = "C")
        val notifs = listOf(posted("B", "10:09:45"))

        val result = StoneWave.classify(stay, notifs, fullCoverage())

        assertEquals(Origin.SELF_STARTED, result.origin)
    }

    // Property: origin is UNKNOWN whenever coverage is false. Seeds used: 1..10.
    @Test
    fun `origin is always UNKNOWN when the look-back is not covered, regardless of notifications`() {
        for (seed in 1L..10L) {
            val random = Random(seed)
            val noCoverage = FakeListenerCoverage() // never opened: coverageFraction is always 0
            val stay = stayAt("10:10:00")
            val notifs = if (random.nextBoolean()) listOf(posted("B", "10:09:45")) else emptyList()

            val result = StoneWave.classify(stay, notifs, noCoverage)

            assertEquals("seed=$seed", Origin.UNKNOWN, result.origin)
        }
    }

    @Test
    fun `noRippleRate excludes neutral packages and counts notifications with no following stay`() {
        val window = Window(
            id = 1L, day = day, start = epochMsAt("10:00:00"), end = epochMsAt("10:30:00"),
            source = WindowSource.INFERRED, partial = false, finalised = true, shape = null,
        )
        val coverage = fullCoverage()
        val notifs = listOf(
            posted("B", "10:05:00"), // no stay follows -> counts against the rate
            posted("C", "10:10:00"), // a stay follows within 60s -> does not count against the rate
            posted("N", "10:15:00"), // neutral package -> excluded entirely
        )
        val stays = listOf(stayAt("10:10:30", firstPkg = "C"))
        val classify: (Pkg) -> AppClass = { pkg -> if (pkg == Pkg("N")) AppClass.NEUTRAL else AppClass.OFF_SET }

        val rate = StoneWave.noRippleRate(window, notifs, coverage, stays, classify)

        assertEquals(0.5, rate!!, 0.0001) // 1 of 2 qualifying notifications had no following stay
    }

    @Test
    fun `noRippleRate is null with no qualifying notifications`() {
        val window = Window(
            id = 1L, day = day, start = epochMsAt("10:00:00"), end = epochMsAt("10:30:00"),
            source = WindowSource.INFERRED, partial = false, finalised = true, shape = null,
        )
        val rate = StoneWave.noRippleRate(window, emptyList(), fullCoverage(), emptyList()) { AppClass.OFF_SET }
        assertNull(rate)
    }
}
