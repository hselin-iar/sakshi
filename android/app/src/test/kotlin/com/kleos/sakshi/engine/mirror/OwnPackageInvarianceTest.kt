package com.kleos.sakshi.engine.mirror

import com.kleos.sakshi.engine.model.AppMeta
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.RawEvent
import com.kleos.sakshi.engine.model.RawType
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.UserClass
import com.kleos.sakshi.engine.ports.Ports
import com.kleos.sakshi.engine.testkit.FakeAppCatalog
import com.kleos.sakshi.engine.testkit.FakeClock
import com.kleos.sakshi.engine.testkit.FakeDerivedStore
import com.kleos.sakshi.engine.testkit.FakeEventStore
import com.kleos.sakshi.engine.testkit.FakeGapStore
import com.kleos.sakshi.engine.testkit.FakeListenerCoverage
import com.kleos.sakshi.engine.testkit.FakeNotifStore
import com.kleos.sakshi.engine.testkit.FakeSayingShelf
import com.kleos.sakshi.engine.testkit.FakeStateStore
import com.kleos.sakshi.engine.testkit.GOLDEN_DATE
import com.kleos.sakshi.engine.testkit.SeededRandomness
import com.kleos.sakshi.engine.testkit.Zones
import com.kleos.sakshi.engine.testkit.epochMsAt
import com.kleos.sakshi.engine.testkit.events
import com.kleos.sakshi.engine.usecases.RecomputeDay
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

/**
 * DOC4's T2.9 property: inserting own-package events never changes any
 * Part. AppClassifier already returns NEUTRAL for the own package
 * unconditionally (T2.3), and NEUTRAL is transparent through
 * DependsResolver/WindowFinder/StayDetector/StretchBuilder, so this should
 * already hold given the existing pipeline -- this pins it down explicitly.
 */
class OwnPackageInvarianceTest {
    private val zone = Zones.KOLKATA
    private val day = StudyDay(GOLDEN_DATE.toEpochDay())
    private val asOf = epochMsAt("23:00:00")
    private val ownPkg = Pkg("com.kleos.sakshi")

    private fun ports(rawEvents: List<RawEvent>): Ports {
        val state = FakeStateStore()
        state.replaceApps(listOf(AppMeta(Pkg("A"), "A", null, UserClass.IN_SET, EpochMs(0))))
        return Ports(
            events = FakeEventStore().apply { append(rawEvents) },
            notifs = FakeNotifStore(),
            coverage = FakeListenerCoverage().apply { openSession(epochMsAt("00:00:00")) },
            gaps = FakeGapStore(),
            derived = FakeDerivedStore(),
            state = state,
            catalog = FakeAppCatalog(own = ownPkg),
            shelf = FakeSayingShelf(),
            clock = FakeClock(asOf),
            random = SeededRandomness(1L),
        )
    }

    @Test
    fun `inserting brief own-package opens never changes windowMinutes, inSetMinutes or quietMinutes`() {
        // A's session is [20:00,21:00] (padded window [19:50,21:10]). Own-package opens are
        // properly paired (RESUMED then PAUSED, like a widget or notification tap) and placed
        // well clear of that window, so they can't disrupt A's own interval via R1 and can't
        // extend any run's gap -- exactly the realistic case the invariant is about.
        val baseStream = events { at("20:00:00") resume ("A"); at("21:00:00") pause ("A") }
        val baseline = RecomputeDay.recompute(day, asOf, ports(baseStream), zone).summary

        for (seed in 1L..10L) {
            val random = Random(seed)
            val count = 1 + random.nextInt(5)
            val ownEvents = (0 until count).flatMap {
                val openMs = epochMsAt("22:00:00").value + it * 60_000L + random.nextInt(30_000)
                val durationMs = 1_000L + random.nextInt(10_000)
                listOf(
                    RawEvent(EpochMs(openMs), RawType.ACTIVITY_RESUMED, ownPkg),
                    RawEvent(EpochMs(openMs + durationMs), RawType.ACTIVITY_PAUSED, ownPkg),
                )
            }
            val withOwn = RecomputeDay.recompute(day, asOf, ports(baseStream + ownEvents), zone).summary

            assertEquals("seed=$seed", baseline.windowMinutes, withOwn.windowMinutes, 0.0001)
            assertEquals("seed=$seed", baseline.inSetMinutes, withOwn.inSetMinutes, 0.0001)
            assertEquals("seed=$seed", baseline.quietMinutes, withOwn.quietMinutes, 0.0001)
        }
    }
}
