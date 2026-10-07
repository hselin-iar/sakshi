package com.kleos.sakshi.engine

import com.kleos.sakshi.engine.model.AppMeta
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.RawEvent
import com.kleos.sakshi.engine.model.RawType
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
import com.kleos.sakshi.engine.testkit.SeededRandomness
import com.kleos.sakshi.engine.testkit.epochMsAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Facade-level checks for processNewEvents(). These deliberately avoid
 * exact calendar-day arithmetic: the facade reads the device's own zone
 * (ZoneId.systemDefault(), see SakshiEngine.kt's comment) rather than a
 * zone this test can inject, so only zone-independent facts are asserted.
 * RecomputeDayTest covers the actual per-day pipeline with an explicit zone.
 */
class SakshiEngineProcessNewEventsTest {

    private fun ports(events: List<RawEvent> = emptyList()): Ports {
        val state = FakeStateStore()
        state.replaceApps(listOf(AppMeta(Pkg("A"), "A", null, UserClass.IN_SET, EpochMs(0))))
        return Ports(
            events = FakeEventStore().apply { append(events) },
            notifs = FakeNotifStore(),
            coverage = FakeListenerCoverage().apply { openSession(epochMsAt("00:00:00")) },
            gaps = FakeGapStore(),
            derived = FakeDerivedStore(),
            state = state,
            catalog = FakeAppCatalog(),
            shelf = FakeSayingShelf(),
            clock = FakeClock(epochMsAt("12:00:00")),
            random = SeededRandomness(1L),
        )
    }

    @Test
    fun `no events ever means nothing recomputed`() {
        val engine = SakshiEngine(ports())
        val report = engine.processNewEvents(epochMsAt("12:00:00"))

        assertEquals(0, report.daysRecomputed)
        assertEquals(0, report.newWindows)
        assertEquals(false, report.baselineFrozen)
    }

    @Test
    fun `a day's worth of activity is recomputed and stored`() {
        val events = listOf(
            RawEvent(epochMsAt("10:00:00"), RawType.ACTIVITY_RESUMED, Pkg("A")),
            RawEvent(epochMsAt("11:00:00"), RawType.ACTIVITY_PAUSED, Pkg("A")),
        )
        val p = ports(events)
        val engine = SakshiEngine(p)

        val report = engine.processNewEvents(epochMsAt("23:00:00"))

        assertTrue(report.daysRecomputed >= 1)
        assertTrue(report.newWindows >= 1)
    }

    @Test
    fun `processing the same events twice is idempotent`() {
        val events = listOf(
            RawEvent(epochMsAt("10:00:00"), RawType.ACTIVITY_RESUMED, Pkg("A")),
            RawEvent(epochMsAt("11:00:00"), RawType.ACTIVITY_PAUSED, Pkg("A")),
        )
        val p = ports(events)
        val engine = SakshiEngine(p)
        val asOf = epochMsAt("23:00:00")

        val first = engine.processNewEvents(asOf)
        val second = engine.processNewEvents(asOf)

        assertEquals(first.daysRecomputed, second.daysRecomputed)
        assertEquals(first.newWindows, second.newWindows)
    }
}
