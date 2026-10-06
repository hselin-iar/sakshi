package com.kleos.sakshi.engine.intervals

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.ForegroundInterval
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.RawEvent
import com.kleos.sakshi.engine.model.RawType
import com.kleos.sakshi.engine.model.ScreenSpan
import com.kleos.sakshi.engine.testkit.epochMsAt
import com.kleos.sakshi.engine.testkit.events
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Golden examples G-I1..G-I5 are the spec (DOC 3, Foreground Intervals and
 * App Classification). Rules R1-R9 verbatim; no invented handling for
 * anything outside them.
 */
class ForegroundIntervalsTest {

    @Test
    fun `G-I1 two switches then screen off`() {
        val stream = events {
            at("10:00:00") resume ("A")
            at("10:05:00") pause ("A")
            at("10:05:00") resume ("B")
            at("10:07:00").nonInteractive()
        }
        val asOf = epochMsAt("10:07:30")
        val result = reconstruct(stream, asOf)

        assertEquals(
            listOf(
                ForegroundInterval(Pkg("A"), epochMsAt("10:00:00"), epochMsAt("10:05:00")),
                ForegroundInterval(Pkg("B"), epochMsAt("10:05:00"), epochMsAt("10:07:00")),
            ),
            result.intervals,
        )
        assertEquals(listOf(ScreenSpan(epochMsAt("10:07:00"), asOf)), result.screenOff)
        assertTrue(result.openEnded)
        assertEquals(0, result.anomalies)
    }

    @Test
    fun `G-I2 sub-second interval is dropped by R7`() {
        val stream = events {
            at("10:00:00") resume ("A")
            at("10:00:00.5") pause ("A")
        }
        val result = reconstruct(stream, epochMsAt("10:00:01"))

        assertEquals(emptyList<ForegroundInterval>(), result.intervals)
        assertEquals(0, result.anomalies)
    }

    @Test
    fun `G-I3 no PAUSED leaves B open to asOf`() {
        val stream = events {
            at("10:00:00") resume ("A")
            at("10:03:00") resume ("B")
        }
        val asOf = epochMsAt("10:05:00")
        val result = reconstruct(stream, asOf)

        assertEquals(
            listOf(
                ForegroundInterval(Pkg("A"), epochMsAt("10:00:00"), epochMsAt("10:03:00")),
                ForegroundInterval(Pkg("B"), epochMsAt("10:03:00"), asOf),
            ),
            result.intervals,
        )
        assertTrue(result.openEnded)
        assertEquals(0, result.anomalies)
    }

    @Test
    fun `G-I4 keyguard and screen-off at the same instant close once`() {
        val stream = events {
            at("10:00:00") resume ("A")
            at("10:02:00").keyguardShown()
            at("10:02:00").nonInteractive()
        }
        val asOf = epochMsAt("10:04:00")
        val result = reconstruct(stream, asOf)

        assertEquals(
            listOf(ForegroundInterval(Pkg("A"), epochMsAt("10:00:00"), epochMsAt("10:02:00"))),
            result.intervals,
        )
        assertEquals(listOf(ScreenSpan(epochMsAt("10:02:00"), asOf)), result.screenOff)
        assertEquals(0, result.anomalies)
    }

    @Test
    fun `G-I5 quick reopen within 2s is joined by R9`() {
        val stream = events {
            at("10:00:00") resume ("A")
            at("10:01:00") pause ("A")
            at("10:01:01") resume ("A")
        }
        val asOf = epochMsAt("10:01:05")
        val result = reconstruct(stream, asOf)

        assertEquals(
            listOf(ForegroundInterval(Pkg("A"), epochMsAt("10:00:00"), asOf)),
            result.intervals,
        )
        assertTrue(result.openEnded)
        assertEquals(0, result.anomalies)
    }

    @Test
    fun `R8 caps an open tail at lastEventTs plus 10 minutes, not at asOf`() {
        val stream = events { at("10:00:00") resume ("A") }
        val farFuture = epochMsAt("12:00:00")
        val result = reconstruct(stream, farFuture)

        val expectedClose = epochMsAt("10:10:00") // lastEventTs (10:00:00) + 10 minutes
        assertEquals(
            listOf(ForegroundInterval(Pkg("A"), epochMsAt("10:00:00"), expectedClose)),
            result.intervals,
        )
        assertTrue(result.openEnded)
    }

    @Test
    fun `PAUSED for a package that is not open is an anomaly and is ignored`() {
        val stream = events {
            at("10:00:00") resume ("A")
            at("10:01:00") pause ("B")
        }
        val result = reconstruct(stream, epochMsAt("10:02:00"))

        assertEquals(1, result.anomalies)
        assertEquals(
            listOf(ForegroundInterval(Pkg("A"), epochMsAt("10:00:00"), epochMsAt("10:02:00"))),
            result.intervals,
        )
    }

    @Test
    fun `double RESUMED for the same already-open package is an anomaly`() {
        val stream = events {
            at("10:00:00") resume ("A")
            at("10:00:05") resume ("A")
        }
        val result = reconstruct(stream, epochMsAt("10:01:00"))

        assertEquals(1, result.anomalies)
    }

    // Property: for any shuffled copy of an event list with distinct timestamps,
    // the Reconstruction is identical; no two intervals overlap; every interval
    // end >= start. Seeds used: 1..10 (reported in the Evidence Package).
    @Test
    fun `shuffled copies of distinct-timestamp streams reconstruct identically`() {
        val seeds = (1L..10L).toList()
        for (seed in seeds) {
            val random = Random(seed)
            val stream = randomDistinctTimestampStream(random, count = 1 + random.nextInt(30))
            val asOf = EpochMs(stream.maxOf { it.ts.value } + 5 * 60_000L)

            val fromOriginalOrder = reconstruct(stream, asOf)
            val fromShuffled = reconstruct(stream.shuffled(random), asOf)

            assertEquals("seed=$seed", fromOriginalOrder, fromShuffled)
            assertNoOverlaps(fromOriginalOrder.intervals, seed)
            fromOriginalOrder.intervals.forEach {
                assertTrue("seed=$seed", it.end.value >= it.start.value)
            }
        }
    }

    private fun randomDistinctTimestampStream(random: Random, count: Int): List<RawEvent> {
        val pkgs = listOf(Pkg("A"), Pkg("B"), Pkg("C"))
        var ts = epochMsAt("09:00:00").value
        val types = RawType.entries
        val stream = mutableListOf<RawEvent>()
        repeat(count) {
            ts += 1_000L + random.nextInt(120_000) // strictly increasing -> distinct timestamps
            val type = types[random.nextInt(types.size)]
            val pkg = if (type == RawType.ACTIVITY_RESUMED || type == RawType.ACTIVITY_PAUSED) {
                pkgs[random.nextInt(pkgs.size)]
            } else {
                null
            }
            stream += RawEvent(EpochMs(ts), type, pkg)
        }
        return stream
    }

    private fun assertNoOverlaps(intervals: List<ForegroundInterval>, seed: Long) {
        val sorted = intervals.sortedBy { it.start.value }
        for (i in 1 until sorted.size) {
            assertTrue("seed=$seed overlap at index $i", sorted[i].start.value >= sorted[i - 1].end.value)
        }
    }
}
