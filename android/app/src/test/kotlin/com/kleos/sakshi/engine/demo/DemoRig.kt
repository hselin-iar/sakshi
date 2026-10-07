package com.kleos.sakshi.engine.demo

import com.kleos.sakshi.engine.SakshiEngine
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.Settings
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
import com.kleos.sakshi.engine.testkit.Zones
import java.time.LocalDate
import java.time.LocalTime

/** A persona's whole history loaded into in-memory ports, with day 0 on a Monday, the way the demo controller does it. */
class DemoRig(val spec: PersonaSpec, seed: Long = 7L, val firstMonday: LocalDate = LocalDate.of(2026, 7, 6)) {
    val own = Pkg("com.kleos.sakshi")
    fun dayStart(i: Int): Long = firstMonday.plusDays(i.toLong()).atTime(LocalTime.of(4, 0)).atZone(Zones.KOLKATA).toInstant().toEpochMilli()
    val history = EventSynthesizer.synthesize(spec, seed, ::dayStart, own)
    val clock = FakeClock(EpochMs(dayStart(0)))
    val ports: Ports

    init {
        val state = FakeStateStore()
        val coverage = FakeListenerCoverage()
        history.sessions.forEach { s -> coverage.openSession(s.connectedAt); s.disconnectedAt?.let { coverage.closeSession(it) } }
        val first = EpochMs(dayStart(history.firstReadDay))
        state.replaceApps(history.apps)
        state.saveSettings(
            Settings(
                studyBlocks = history.studyBlocks, learnStudyHours = false, gentleMode = spec.gentleDefault, gentleExplicit = spec.gentleDefault,
                weeklyNoteEnabled = false, ageUnder18 = false, batteryHelperShown = true, lapseAcknowledgedThrough = null,
                firstReadAt = first, createdAt = EpochMs(dayStart(0)),
            ),
        )
        state.saveIngest(state.ingest().copy(firstReadAt = first))
        val events = FakeEventStore().apply { append(history.events) }
        val notifs = FakeNotifStore().apply { history.notifs.forEach { append(it) } }
        ports = Ports(events, notifs, coverage, FakeGapStore(), FakeDerivedStore(), state, FakeAppCatalog(history.launcherApps), FakeSayingShelf(), clock, SeededRandomness(seed))
    }

    val engine = SakshiEngine(ports, isDemo = true)

    /** The instant just before study day `day` + 1 begins: what the demo's slider uses. */
    fun endOf(day: Int) = EpochMs(dayStart(day + 1) - 60_000L)
}
